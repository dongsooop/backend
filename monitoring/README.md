# API 사용량 모니터링 — 설치·운영 가이드

백엔드가 요청마다 사용 기록을 Elasticsearch `api-usage-YYYY.MM` 인덱스에 쌓고, Grafana 대시보드로 보고, 매주 월요일 09:00 디스코드로 주간 리포트를 보낸다. 설계 배경은 `docs/specs/2026-09-18-api-usage-monitoring-design.md`.

## 구성 요소

| 구성 | 위치 | 역할 |
|---|---|---|
| 요청 기록 인터셉터·레코더 | 백엔드 `monitoring/` 패키지 | 요청 1건 → 문서 1건. 5초마다 ES bulk 저장 |
| 보존 스케줄러 | 백엔드 | 기본 꺼짐(계속 보관). `retention-months`를 N으로 주면 매일 04:00 N개월 지난 월 인덱스 삭제 |
| 주간 리포트 스케줄러 | 백엔드 | 매주 월 09:00, ES 집계 → 디스코드 웹훅 |
| Grafana | 이 폴더의 compose | ES를 읽어 대시보드 표시. nginx `/grafana/` 로 접근 |

Prometheus·Logstash·Kibana는 쓰지 않는다. 기존 `boards` 인덱스와 검색 기능은 건드리지 않는다.

## 1. 백엔드 환경변수 (`/home/ubuntu/dongsoop/config/.env.prod`)

```
MONITORING_USAGE_ENABLED=true                 # 생략하면 prod 기본값 true
USAGE_REPORT_DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/...   # 없으면 리포트만 안 나감
```

배포는 평소처럼(`main` 병합 → 깃액션 → `deploy.sh`). 배포 직후 컨테이너 로그에 `Registered index template api-usage` 가 한 번 찍히면 정상이다. 이미 템플릿이 있으면 아무 로그도 없다.

확인:
```
curl -s localhost:9200/_cat/indices/api-usage-*?v      # 요청이 몇 번 들어온 뒤 문서 수(docs.count)가 늘어야 한다
curl -s 'localhost:9200/api-usage-*/_search?size=1&pretty'
```

## 2. Grafana 띄우기

```
# 서버에서
cd /home/ubuntu/dongsoop
# 레포의 monitoring/ 폴더를 이 위치에 복사해 둔다 (scp 또는 git pull)
cp monitoring/.env.example config/.env.grafana
vi config/.env.grafana    # GRAFANA_ADMIN_PASSWORD 채우기. 나머지는 서버 값이 이미 들어 있다
docker compose --env-file config/.env.grafana -f monitoring/docker-compose.monitoring.yml up -d
docker logs grafana --tail 50    # "HTTP Server Listen" 이 보이면 기동 완료
```

env 파일은 백엔드의 `config/.env.prod` 옆에 둔다. 재시작·업데이트 때도 항상 `--env-file config/.env.grafana` 를 붙인다(빠지면 비밀번호가 비었다는 에러로 기동하지 않는다). `monitoring/.env` 에 두고 `--env-file` 없이 띄워도 된다.

`config/.env.grafana` 값:

| 키 | 값 |
|---|---|
| `GRAFANA_ADMIN_PASSWORD` | 팀에서 정한 비밀번호 |
| `APP_NETWORK` | `dongsoop_app-network` (server-blue/green·nginx·elasticsearch가 모두 붙어 있는 네트워크. 바뀌었으면 `docker network ls`) |
| `ES_URL` | `http://elasticsearch:9200` (컨테이너 이름 `elasticsearch`, 8.12.0) |
| `GRAFANA_ROOT_URL` | `https://dongsoop.site/grafana/` (도메인이 다르면 맞춰서) |

## 3. nginx 에 `/grafana/` 추가 (`/home/ubuntu/dongsoop/nginx/conf.d/default.conf`)

`server { ... }` 블록 안, `location /api` 와 `location / ` 사이에 넣는다(`location /` 보다 앞에 있어야 `/grafana/` 가 웹으로 안 넘어간다):

```
location /grafana/ {
    # 컨테이너 이름을 변수로 두면 grafana 가 아직 없어도 nginx -t 가 통과한다.
    # deploy.sh 가 배포마다 nginx 를 reload 하므로, 이름을 직접 쓰면 grafana 가 꺼진 순간 배포가 실패한다.
    resolver 127.0.0.11 valid=10s;
    set $grafana_upstream http://grafana:3000;
    proxy_pass $grafana_upstream;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

`proxy_pass` 주소 끝에 슬래시를 붙이지 않는다. Grafana가 `/grafana/` 접두어를 그대로 받도록 설정(`GF_SERVER_SERVE_FROM_SUB_PATH=true`)돼 있다. nginx 컨테이너가 grafana와 같은 네트워크에 있어야 이름으로 붙는다.

**순서 주의**: Grafana 컨테이너를 먼저 띄운 뒤 이 블록을 넣고 reload 한다. 2026-09-18 배포 때 블록을 먼저 넣어 두는 바람에 `deploy.sh` 의 nginx reload 가 "host not found in upstream grafana" 로 실패한 적이 있다. 위처럼 변수 방식이면 그 경우에도 검사는 통과하고 `/grafana/` 만 502 가 된다.

```
docker exec nginx nginx -t && docker exec nginx nginx -s reload
```

브라우저에서 `https://dongsoop.site/grafana/` → admin 로그인 → 왼쪽 Dashboards → 폴더 "동숲" → "동숲 API 사용량".

## 4. 디스코드 알림 규칙 (Grafana UI에서 한 번 설정)

1. **Alerting → Contact points → Add**: Integration `Discord`, Webhook URL 입력, 이름 `discord`.
2. **Alerting → Notification policies**: Default policy의 contact point를 `discord`로.
3. **Alerting → Alert rules → New alert rule** 세 개:

| 이름 | 쿼리 (datasource dongsoop-usage) | 조건 | 평가 |
|---|---|---|---|
| 5xx 급증 | Lucene `status:>=500`, Metric Count, 버킷 없음 | Threshold: IS ABOVE 20 | 매 1분, pending 5분 |
| 응답 지연 | Metric Percentiles(durationMs, 95), 버킷 없음 | IS ABOVE 3000 | 매 1분, pending 5분 |
| 기록 끊김 | Metric Count, 버킷 없음 | IS BELOW 1 | 매 1분, pending 1시간, 시간 범위 now-1h. No data 도 Alerting 으로 |

앞 두 규칙의 시간 범위(Options → Time range)는 "now-5m to now"로 둔다. 기록 끊김은 트래픽이 적은 시간대에 오탐이 나서 1시간 기준이다. 임계치는 첫 주 데이터를 보고 조정한다.

2026-09-18 서버에는 이 세 규칙이 Grafana 설정 API(`/api/v1/provisioning/alert-rules`)로 이미 등록돼 있다(폴더 "동숲 알림", 그룹 dongsoop). 디스코드에 `[FIRING:1] DatasourceError` 가 오면 규칙이 아니라 데이터소스 설정 문제다.

## 5. 운영

- **용량 확인**: `curl -s 'localhost:9200/_cat/indices/api-usage-*?v&h=index,docs.count,store.size'`. 문서 1건 ≈ 150~300B. 주 5만 요청이면 3개월에 100~200MB.
- **보존 기간**: 기본은 삭제하지 않음(`monitoring.usage.retention-months: 0`). 디스크가 부담되면 값을 개월 수로 바꿔 재배포하면 다음 04:00부터 오래된 월 인덱스를 지운다. 문서 1건 ≈ 150~300B라 연 수백 MB 수준.
- **기록 끄기**: `MONITORING_USAGE_ENABLED=false` 후 재배포. Grafana는 그대로 둬도 된다.
- **기능 라벨**: 새 컨트롤러 경로가 추가되면 `monitoring/constant/FeatureLabel.java` 표에 한글 이름을 넣는다. 안 넣으면 경로 키(`project-board`)가 그대로 보인다.
- **주간 리포트 즉시 확인**: 배포 후 월요일을 기다리지 않고 보려면 서버에서 스케줄 시각을 기다리는 수밖에 없다. 로컬에서는 `UsageReportScheduler.sendReportEndingAt(LocalDate)` 를 테스트로 호출한다.

## 6. 문제 해결

| 증상 | 확인 |
|---|---|
| 인덱스가 안 생김 | 백엔드 로그에 `Api usage bulk failed` 가 있으면 ES 연결 문제. `ELASTICSEARCH_URIS` 와 ES 컨테이너 상태 |
| `Api usage queue is full` 경고 | ES 쓰기가 5초 안에 못 따라감. ES 부하 확인. 그동안의 기록은 버려지지만 서비스에는 영향 없음 |
| Grafana 대시보드가 비어 있음 | 데이터소스 설정 → Save & test. `docker exec grafana wget -qO- http://elasticsearch:9200` 이 응답하는지 |
| `/grafana/` 가 404 | nginx location 누락 또는 reload 안 함. `GRAFANA_ROOT_URL` 과 실제 접속 주소 불일치 |
| 리포트가 안 옴 | `USAGE_REPORT_DISCORD_WEBHOOK_URL` 비어 있음, 또는 로그 `Usage report query failed` |

## 로컬에서 확인하기

```
docker run -d --name es-local -p 9200:9200 -e discovery.type=single-node -e xpack.security.enabled=false \
  -e ES_JAVA_OPTS="-Xms512m -Xmx512m" elasticsearch:8.15.5
docker start dongsoop-redis   # 없으면 docker run -d --name dongsoop-redis -p 6379:6379 redis:7
./gradlew bootRun --args='--spring.profiles.active=local --spring.data.elasticsearch.repositories.enabled=true \
  --spring.elasticsearch.uris=http://localhost:9200 --monitoring.usage.enabled=true --appcheck.ignore-path=/**'
curl -s localhost:8080/meal/current > /dev/null; sleep 6
curl -s 'localhost:9200/api-usage-*/_search?pretty&size=1'
```

Grafana도 로컬로 보려면 compose의 `ports` 주석을 풀고 `.env`에 `APP_NETWORK=bridge`, `ES_URL=http://host.docker.internal:9200`, `GRAFANA_ROOT_URL=http://localhost:3000/` 로 띄운 뒤 `GF_SERVER_SERVE_FROM_SUB_PATH` 를 `false` 로 바꾼다.
