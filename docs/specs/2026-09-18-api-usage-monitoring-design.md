# API 사용량 모니터링 — 설계 기획서

작성일: 2026-09-18
대상 레포: dongsooop/backend
상태: 구현 완료 (2026-09-18, 브랜치 feat/api-usage-monitoring). 설치·운영 절차는 `monitoring/README.md`.

---

## 1. 배경과 목표

지금 백엔드에는 "어떤 기능을 얼마나 많은 사용자가 쓰는지"를 알 수 있는 기록이 없다. 로그는 앱 로그(INFO 이상)뿐이고, 요청 단위 기록이나 메트릭 의존성이 없다. 기기 마지막 접속 시각만 갱신된다.

이 작업으로 다음 셋을 얻는다.

1. **기능별 사용량**: 모듈(게시판·채팅·소개팅·이클래스 등 27개)과 엔드포인트(155개) 단위로 호출 수·**유니크 사용자 수**·응답시간·에러율을 계속 쌓는다.
2. **디스코드 주간 리포트**: 매주 월요일 아침, 지난 7일 기능별 사용자 수·호출 수와 전주 대비 증감을 디스코드로 보낸다.
3. **모니터링 대시보드·알림**: Grafana 대시보드로 추세를 보고, 에러율·응답시간 임계치 알림을 디스코드로 받는다.

**비목표 (이번 범위 밖)**
- JVM 메모리·GC·DB 커넥션 풀 같은 서버 내부 지표. 필요해지면 Micrometer + Prometheus를 얹는다. 이번 설계와 충돌하지 않는다.
- 화면 진입 이벤트(앱·웹 클라이언트 SDK). 서버 API 호출로만 본다.
- 분산 트레이싱. SigNoz 같은 APM은 램 2~3GB를 먹어 뺀다.
- 과거 데이터 역산. 배포 시점부터 쌓인다.
- Logstash·Kibana 변경. 건드리지 않는다.

## 2. 왜 이 방식인가

| 후보 | 판단 |
|---|---|
| New Relic 무료(월 100GB) | 코드 변경 최소지만 사용자 단위 집계·디스코드 리포트 없음. 데이터가 외부로 나감 |
| Grafana Cloud 무료 | 보존 14일이라 월간 추세 불가 |
| Sentry 무료 | 에러 추적용. 호출량 집계에는 한도가 작음 |
| PostHog | 사용자 단위 분석에 좋지만 클라이언트 SDK 작업 필요 |
| Prometheus + Grafana 자체 호스팅 | 호출량·응답시간은 되지만 메트릭에 사용자 ID를 못 넣어 유니크 사용자가 안 나옴 |
| **기존 Elasticsearch에 사용 기록 저장 + Grafana** | 이미 운영 중인 ES 클라이언트 재사용, 새 컨테이너는 Grafana 하나, 유니크 사용자까지 나옴, 데이터가 서버 밖으로 안 나감 → **채택** |

## 3. 구성

```
[앱/웹] → nginx → server-blue / server-green (기존)
                      │ 요청마다 인터셉터가 사용 기록 1건을 큐에 넣음
                      │ 5초마다 묶어서 bulk 저장 (ES 죽어도 요청에 영향 없음)
                      ▼
               elasticsearch (기존)  인덱스 api-usage-YYYY.MM, 보존 3개월
                      │
                      ▼
               grafana (:3000, 신규) ── nginx /grafana/ ── 팀 브라우저
                      │
                      └─ 알림 → 디스코드 웹훅

[백엔드 스케줄러] 매주 월 09:00 → ES 집계 → 디스코드 웹훅 (주간 리포트)
[백엔드 스케줄러] 매일 04:00 → retention-months 지난 월 인덱스 삭제 (기본 0 = 삭제 안 함, 2026-09-18 결정)
```

### 3.1 사용 기록 문서 (ES `api-usage-YYYY.MM`)

| 필드 | 타입 | 값 |
|---|---|---|
| `@timestamp` | date | 응답 완료 시각 |
| `method` | keyword | GET/POST/… |
| `uri` | keyword | 핸들러 패턴. `/board/{boardId}` 처럼 path variable이 합쳐진 형태. 매핑 없으면 `UNKNOWN` |
| `feature` | keyword | `uri` 첫 세그먼트 (`project-board`, `chat`, `eclass`, …). `/v2/...` 처럼 버전 접두어가 있으면 그 다음 세그먼트. 기능별 묶음 키 |
| `status` | integer | HTTP 상태코드 |
| `durationMs` | long | 처리 시간 |
| `memberId` | long | 로그인 요청이면 회원 ID(principal). 아니면 없음 |
| `deviceId` | long | JWT 요청이면 기기 ID(details). 아니면 없음 |
| `actor` | keyword | 유니크 사용자 집계 키. `m:{memberId}` > `d:{deviceId}` > `f:{X-Device-Fid}` 순으로 하나. 셋 다 없으면 `anon` |

카디널리티: `uri`는 패턴이라 155개 안팎, `actor`는 사용자 수만큼. 문서 1건 ≈ 200B, 주 5만 요청이면 월 40MB 수준이다.

### 3.2 백엔드 (신규 패키지 `monitoring/`)

| 파일 | 역할 |
|---|---|
| 설정 (`application.yml` `monitoring.*`) | `monitoring.usage.enabled`(기본 false, prod true), `monitoring.discord.webhook-url`, `monitoring.usage.retention-months`(기본 3). 각 클래스가 `@Value`로 읽는다 |
| `monitoring/config/MonitoringWebConfig` | `WebMvcConfigurer`로 인터셉터 등록. `enabled=true`일 때만. `/error`(재전달로 이중 집계)와 `/health`(도커 헬스체크 노이즈)는 제외 |
| `monitoring/interceptor/ApiUsageInterceptor` | `preHandle`에서 시작 시각을 request attribute에 저장, `afterCompletion`에서 문서를 만들어 `ApiUsageRecorder.offer()`. `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE`로 uri 패턴, `SecurityContextHolder`로 member/device, 헤더로 fid. 예외는 삼키고 debug 로그 |
| `monitoring/dto/ApiUsageEvent` | record. 3.1 필드 |
| `monitoring/service/ApiUsageRecorder` | 유계 큐(10,000). `@Scheduled(fixedDelay = 5s)`로 비워서 `ElasticsearchClient.bulk()`. 큐가 차면 새 이벤트 버림(warn 1회/분), bulk 실패는 warn 후 버림. 앱 시작 시 `_index_template/api-usage` 멱등 등록 |
| `monitoring/scheduler/ApiUsageRetentionScheduler` | 매일 04:00, `api-usage-*` 중 `retention-months` 지난 월 인덱스 삭제 |
| `monitoring/client/UsageStatsClient` | ES 집계 요청 한 번을 `UsageWindow`(호출·사용자·기능별·p95·5xx)로 변환. ES 접근은 여기에만 |
| `monitoring/service/UsageReportService` + `Impl` | `UsageStatsClient`를 이번 주·지난 주 두 번 불러 `UsageReport` 생성. 증감 계산은 순수 함수 |
| `monitoring/dto/UsageReport`, `FeatureUsage` | record. 전체 호출·유니크 사용자, 기능별 (feature, users, calls, usersDeltaPercent), 느린 API 1개, 5xx 합계·최다 경로 |
| `monitoring/util/UsageReportMessage` | `UsageReport` → 디스코드 본문. 기능 상위 10개(유니크 사용자 순) + 눈에 띄는 것 |
| `monitoring/scheduler/UsageReportScheduler` | `@Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Seoul")`. 웹훅 URL 비어 있으면 아무것도 안 함. ES 조회 실패 시 warn 후 스킵(부분 리포트 안 보냄) |
| `monitoring/client/DiscordWebhookClient` | 기존 `RestTemplate`으로 `POST {webhookUrl}` `{"content": "..."}` |
| `monitoring/constant/FeatureLabel` | `feature` 키 → 한글 라벨 표 (`project-board → 프로젝트 모집`). 표에 없으면 키 그대로 |

집계 쿼리(지난 7일, `@timestamp` range):
- 전체 호출 = 문서 수, 유니크 사용자 = `cardinality(actor)`
- 기능별 = `terms(feature)` + 서브 `cardinality(actor)`. 정렬은 사용자 수
- 전주 대비 = 같은 집계를 7~14일 전 범위로 한 번 더. 전주 0이면 "신규"
- 느린 API = `terms(uri)` + `percentiles(durationMs, 95)` 상위 1개
- 5xx = `status >= 500` 필터 문서 수 + `terms(uri)` 상위 1개

디스코드 메시지 예:
```
📊 동숲 주간 사용 리포트 (9/11 ~ 9/17)
사용자 1,102명 (▲4%) · API 호출 48,213회 (▲12%)

기능별 사용자 수
 1. 게시판    812명  ▲9%   (14,820회)
 2. 홈        790명  ▲3%
 3. 채팅      401명  ▲21%
 ...
이번 주 눈에 띄는 것
 • 소개팅 ▲65%
 • 가장 느린 API: GET /board/{boardId}/comments p95 1.8s
 • 5xx 37건, 최다: POST /chat/rooms/{roomId}/messages
```

로컬·테스트 프로필은 ES가 꺼져 있어(`spring.data.elasticsearch.repositories.enabled=false`) `ElasticsearchClient` 빈이 없다. `monitoring.usage.enabled`가 기본 false라 인터셉터·레코더·스케줄러가 뜨지 않고, 켜더라도 `ObjectProvider<ElasticsearchClient>`로 받아 빈이 없으면 조용히 비활성화한다.

### 3.3 서버 (레포 `monitoring/` 폴더 → 서버 `/home/ubuntu/dongsoop/monitoring/`로 복사)

```
monitoring/
  docker-compose.monitoring.yml            # grafana 하나. 기존 앱 네트워크에 external로 참여
  .env.example                             # 비밀번호·네트워크 이름·ES 주소. .env 는 gitignore
  grafana/provisioning/datasources/elasticsearch.yml   # index api-usage-*, time field @timestamp. URL은 env ES_URL
  grafana/provisioning/dashboards/dongsoop.yml         # 아래 JSON을 "동숲" 폴더로 로드
  grafana/dashboards/dongsoop-usage.json
  README.md                                # 설치·운영 절차, 알림 규칙 설정법
```

알림 규칙은 프로비저닝 파일 대신 Grafana UI에서 만든다(README 4절). 프로비저닝 YAML은 스키마가 까다로워 잘못 쓰면 기동 자체가 막히는데 지금 검증할 Grafana가 없어서다. 규칙 세 개라 UI 설정이 5분이면 끝난다.

- Grafana: 볼륨 `grafana-data`, `GF_SECURITY_ADMIN_PASSWORD` env, `GF_SERVER_ROOT_URL=https://dongsoop.site/grafana/`, `GF_SERVER_SERVE_FROM_SUB_PATH=true`. nginx에 `location /grafana/ { proxy_pass http://grafana:3000; }` 추가(끝 슬래시 없음, 접두어 유지)(사용자가 서버에서 적용). Grafana 자체 로그인으로 팀만 접근.
- ES 컨테이너 이름과 앱 네트워크 이름은 서버에서 `docker ps`·`docker network ls`로 확인해 `.env`에 넣는다.

**대시보드 패널** (상단 변수 `기능`으로 전 패널 필터, 기본 "All")
1. 상단 숫자 4개: 유니크 사용자, 호출 수, 5xx 건수, p95
2. 기능별 유니크 사용자 시계열(일 단위, `terms feature` + `cardinality actor`, `anon` 제외)
3. 기능별 호출량 막대
4. 엔드포인트 순위표: 사용자 수·호출 수·p95. 정렬·검색 가능, "All"이면 155개 전부
5. 상태코드 분포, 시간대별 호출

**알림 규칙** (5분 창, 디스코드 contact point)
- 5xx 건수 > 20 (5분)
- p95 > 3s (5분)
- 최근 5분 문서 0건 (백엔드 죽었거나 기록이 끊김), No data도 알림

## 4. 배포 순서 (사용자 수동)

1. 백엔드 브랜치 병합·배포. `config/.env.prod`에 `USAGE_REPORT_DISCORD_WEBHOOK_URL=<웹훅>` 추가(기록 자체는 prod 기본 on, 끄려면 `MONITORING_USAGE_ENABLED=false`). 웹훅 없이 배포해도 기록은 쌓이고 리포트만 안 나간다.
2. `monitoring/` 서버 복사 → `docker compose -f monitoring/docker-compose.monitoring.yml up -d`.
3. nginx `/grafana/` location 추가 후 reload. 브라우저에서 접속해 대시보드 확인.
4. Grafana에서 디스코드 contact point 생성(웹훅 URL 입력).
5. 다음 월요일 09:00 리포트 도착 확인.

## 5. 검증

- 단위 테스트: `ApiUsageInterceptor`(패턴·actor 우선순위·anon·UNKNOWN), `ApiUsageRecorder`(큐 초과 시 버림, bulk 실패 시 예외 전파 없음), `UsageReportMessage` 포맷(증감 부호·순위·신규), `UsageReportServiceImpl` 증감 계산, `UsageReportScheduler`(웹훅 비면 ES 미호출, ES 실패 시 웹훅 미호출), `ApiUsageRetentionScheduler` 삭제 대상 계산.
- 로컬 통합(2026-09-18 수행): 도커 ES 8.15.5(+analysis-nori)로 `bootRun` → 요청 7건 중 `/health`(제외)와 보안 필터에서 401로 막힌 2건을 뺀 4건이 정확히 4문서로 저장됨. fid 헤더 비회원은 `f:...`로 식별. `UsageStatsClientLocalIT`(`ES_LOCAL=1`일 때만 실행)로 집계 쿼리 파싱과 리포트 문장 생성 확인.
- 보안 필터에서 거절된 요청(401/403)은 컨트롤러까지 오지 않아 기록되지 않는다. 기능 사용이 아니므로 의도한 동작.
- Grafana 대시보드 렌더는 로컬에서 확인하지 않음(JSON 문법·compose 설정만 검증). 서버에 올린 뒤 첫 확인이 필요하다.
- 전체 테스트 603건 통과(실패 0, 스킵 4는 기존).

## 6. 건드리지 않는 것

기존 필터·보안 경로, 발송 경로, `deploy.sh`·깃액션 흐름, 기존 스케줄러, ES `boards` 인덱스와 검색 코드, Logstash.

## 7. 후속 (이번 범위 밖)

- JVM·DB 풀 지표: Micrometer + Prometheus 컨테이너 추가, 같은 Grafana에 데이터소스만 추가.
- 웹 BFF 자체 메트릭.
