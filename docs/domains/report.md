# Report

> 관련 패키지: `src/main/java/com/dongsoop/dongsoop/report/**`

## Responsibility

- 게시판(프로젝트·스터디·마켓플레이스·과외) 신고, 회원 신고, 채팅 메시지 신고(일반 채팅·과팅)를 접수한다.
- 게시판 신고와 일반 채팅 신고에 대해 욕설 필터로 자동 판정하고, 판정되지 않은 신고는 관리자가 조치한다.
- 관리자 제재(경고·일시정지·영구정지·게시글 삭제·채팅방 추방)와 제재 없이 신고를 닫는 기각을 처리한다.
- 회원 본인의 제재 상태 조회를 제공한다.

## Out of Scope

- 채팅방 참여·초대·자진 퇴장과 방장의 추방 규칙 → `chat` 패키지(도메인 문서 미작성). 이 문서는 관리자가 신고 처리로 실행하는 `CHAT_KICK` 제재만 다룬다.
- 과팅 진행 흐름(단계·시간·얼리기)과 과팅 메시지 보관소 자체의 규칙 → `blinddate` 패키지(도메인 문서 미작성. 생기면 메시지 보관 규칙은 그쪽으로 옮긴다). 이 문서는 그 보관소에서 신고 시점에 무엇을 복사해 오는지만 다룬다.
- 회원 간 차단 관계, 전송 차단, 게시판 목록 차단 필터 → `memberblock.md`

## Terminology

### Report (신고)

- 의미: `Report` 엔티티 한 건. `reportType`으로 대상을 구분한다 — 게시판 4종(`PROJECT_BOARD`, `STUDY_BOARD`, `MARKETPLACE_BOARD`, `TUTORING_BOARD`), `MEMBER`, `CHAT_MESSAGE`(일반 채팅), `BLINDDATE_MESSAGE`(과팅).
- 코드: `ReportType`, `Report.reportType`

### Sanction (제재)

- 의미: 신고 처리 결과로 대상 회원에게 실행하는 조치. `SanctionType`은 `WARNING`(경고), `TEMPORARY_BAN`(일시정지), `PERMANENT_BAN`(영구정지), `CONTENT_DELETION`(게시글 삭제), `CHAT_KICK`(채팅방 추방)이다.
- 코드: `Sanction` 엔티티, `Report.sanction`

### 메시지 스냅샷

- 의미: 채팅 메시지 신고 시점에 복사해 두는 신고 대상 메시지의 내용·발신자·전송 시각. 원본 메시지가 나중에 삭제되거나 만료돼도 신고 처리에 필요한 증거로 남는다.
- 코드: `Report.messageContent`, `Report.messageSentAt`, `ChatMessageSnapshot`

### 맥락 메시지

- 의미: 신고된 메시지 직전의 사용자 메시지를 최대 10개까지 복사한 것. 관리자가 앞뒤 맥락을 보고 판단하도록 돕는다. 입장·퇴장 시스템 메시지는 포함하지 않는다.
- 코드: `Report.messageContext`(`ChatMessageSnapshots`)

### 자동 판정

- 의미: 자동제재 스케줄러가 욕설 필터로 이미 판단을 마친 신고인지 여부. `true`면 스케줄러가 다시 집지 않고 관리자 미처리 목록에만 남는다.
- 코드: `Report.isAutoReviewed`, `is_auto_reviewed` 컬럼

### 기각

- 의미: 관리자가 제재 없이 신고를 처리 완료 상태로 닫는 것. 욕설이 아니어서 자동 판정을 통과하지 못한 채팅·과팅 신고를 닫는 데 쓴다.
- 코드: `Report.dismiss()`, `POST /reports/{reportId}/dismiss`

## Main Flow

일반 채팅 신고와 자동 판정:

```text
POST /reports/chat { roomId, messageId, reason, description }
  ↓
참여자 검증 → 메시지 조회(Redis 우선, 없으면 chat_messages 백업) → 시스템 메시지·자기 신고·중복 거절
  ↓
메시지 스냅샷 + 직전 맥락 메시지(최대 10개) 복사 저장 → 201
  ↓
AutoSanctionScheduler(1시간마다, 최대 3건, is_auto_reviewed=false인 CHAT_MESSAGE만 조회)
  ↓
욕설 판정 → 욕설이면 자동 WARNING 생성 + 처리 완료 + 경고 누적 검사(3·5·7회 → 3·14·30일 자동 정지)
        → 욕설이지만 같은 메시지에 이미 경고가 있으면 새 경고 없이 처리 완료
        → 욕설이 아니거나 API 실패면 처리하지 않고 is_auto_reviewed만 true로 표시
  ↓
관리자 목록(GET /reports/admin)에서 미처리 신고 확인 → POST /reports/sanctions 또는 POST /reports/{id}/dismiss
```

과팅 메시지 신고(자동 판정 없음):

```text
과팅 세션 중 메시지 브로드캐스트 → 세션별 메시지 보관소에 저장(과팅 도메인 소유)
  ↓
POST /reports/blinddate { messageId, reason, description }
  ↓
신고자의 참여 세션 확인 → 보관소에서 messageId 조회(세션 종료 후면 없음) → 자기 신고·같은 세션 내 같은 상대 중복 거절
  ↓
메시지 스냅샷 + 보관소의 직전 맥락 메시지(최대 10개) 복사 저장 → 201
  ↓
관리자가 GET /reports/admin에서 확인 후 세션 종료 뒤 POST /reports/sanctions 또는 기각으로 처리
```

## Domain Rules

- 채팅 메시지 신고는 시스템 메시지(입장·퇴장)를 대상으로 할 수 없고, 발신자 본인은 신고할 수 없다.
- 일반 채팅 신고는 같은 신고자·같은 메시지 조합으로 중복 신고할 수 없다(`reporter_id, message_id` 유니크).
- 과팅 신고는 같은 신고자가 같은 세션에서 같은 상대를 중복 신고할 수 없다(`reporter_id, chat_room_id, target_member_id` 유니크, `report_type = 'BLINDDATE_MESSAGE'`일 때만 적용). 세션이 끝나 메시지 보관소가 비워진 뒤에는 신고할 수 없다.
- 자동제재 스케줄러는 게시판 4종과, 아직 자동 판정되지 않은(`is_auto_reviewed = false`) 일반 채팅 신고만 조회한다. 과팅 신고(`BLINDDATE_MESSAGE`)와 회원 신고(`MEMBER`)는 자동 판정 대상이 아니다.
- 경고는 영구 누적된다. 누적 3·5·7회에 도달하면 각각 3·14·30일 자동 정지가 붙는다. 8회 이상은 자동 제재 없이 관리자가 판단한다.
- `TEMPORARY_BAN`은 관리자가 종료일을 반드시 입력해야 한다(없으면 400). `WARNING`·`PERMANENT_BAN`은 종료일 입력이 없으면 9999-12-31로 채운다(경고는 영구 누적이 정책이라 만료시키지 않기 위함). `CONTENT_DELETION`·`CHAT_KICK`은 기간 개념이 없어 시작 시각을 종료일로 쓴다.
- `CHAT_KICK`은 `CHAT_MESSAGE` 신고이면서 대상 방이 그룹방일 때만 허용한다. 그 외에는 400으로 거절한다. 대상이 방장이면 거절하고(방장 부재 방지), 이미 방을 나간 대상은 퇴장 시스템 메시지 없이 제재만 기록한다.
- 메시지 신고(`CHAT_MESSAGE`, `BLINDDATE_MESSAGE`)의 관리자 제재는 요청한 제재 대상 회원이 신고된 메시지의 작성자와 같아야 한다(다르면 400).
- 메시지 신고에는 `CONTENT_DELETION`(게시글 삭제) 제재를 쓸 수 없다(400). 삭제할 게시글이 없기 때문이다.
- 신고 기각은 이미 처리된 신고에는 쓸 수 없다(409).
- 회원 본인의 제재 상태 조회(`GET /reports/sanction-status`)는 활성 상태인 정지(`TEMPORARY_BAN`, `PERMANENT_BAN`)만 노출하고, 여러 건이 활성 상태면 종료일이 가장 늦은 것을 반환한다. 경고는 영구 누적이라 노출하면 한 번 경고받은 사용자가 계속 "제재 중"으로 보이므로 노출하지 않는다.
- 신고자에게는 처리 결과를 통보하지 않는다. 신고 접수 응답(201)만 준다.
- 게시판 신고의 판정·처리 방식(욕설 필터 호출, 실패 시 처리 완료 취급)은 이번 개편에서 바꾸지 않는다.

## Decisions

### 채팅·과팅 메시지를 신고 시점에 복사해서 저장한다

#### 선택

`Report`에 `messageContent`, `messageSentAt`, `messageContext`(직전 최대 10개 메시지) 컬럼을 두고 신고 접수 시점에 원본에서 복사한다.

#### 이유

일반 채팅 메시지는 Redis에 30일만 보관되고 이후 만료되며, 과팅 메시지는 세션이 끝나면 보관소 자체가 비워진다. 원본을 참조만 하면 관리자가 나중에 확인할 때 이미 사라져 있을 수 있어, 신고 시점의 사본을 별도로 남긴다.

#### 영향 / Trade-offs

- `Report` 테이블에 게시판 신고에서는 항상 null인 컬럼 6개가 추가된다.
- 메시지 내용이 신고 이후 원본과 별개로 고정되므로, 신고 이후 원본이 삭제·수정돼도 관리자는 신고 시점 내용을 본다.

### 욕설이 아닌 채팅 신고는 닫지 않는다

#### 선택

일반 채팅 신고를 욕설 필터로 판정해 욕설이 아니거나 필터 API 호출이 실패하면, 신고를 처리 완료로 닫지 않고 `is_auto_reviewed`만 `true`로 표시해 관리자 미처리 목록에 남긴다(`AsyncAutoSanctionService.judgeChatMessage`).

#### 이유

스팸·사기·개인정보 침해처럼 욕설 필터로는 판단할 수 없는 신고 사유가 있다. 또한 기존 게시판 신고 로직은 필터 API 장애를 "욕설 없음"과 동일하게 취급해 신고를 그대로 처리 완료해 버리는데(`checkProfanityAndExecute`), 이 방식을 채팅에서는 반복하지 않기 위해 실패 시에도 관리자에게 넘긴다.

#### 영향 / Trade-offs

- 게시판 신고의 기존 동작(API 실패 시 처리 완료)은 그대로 둔다. 같은 서비스 안에 서로 다른 두 정책이 공존한다.
- 관리자 미처리 목록이 늘어날 수 있다.

### 자동제재 스케줄러의 조회 대상을 좁힌다

#### 선택

`findUnprocessedReports`가 조회하는 미처리 신고를 "게시판 4종 + `is_auto_reviewed = false`인 `CHAT_MESSAGE`"로 한정한다. 과팅 신고와 회원 신고는 조회 대상에서 뺀다.

#### 이유

기존 쿼리는 모든 미처리 신고를 가져갔는데, 게시판이 아닌 신고는 게시글을 찾지 못해 빈 문자열로 판정되어 "욕설 없음"으로 잘못 처리 완료됐다. 과팅 신고와 회원 신고는 애초에 욕설 필터로 자동 판단할 대상이 아니다.

#### 영향 / Trade-offs

- 회원 신고(`MEMBER`)는 더 이상 자동으로 처리 완료되지 않고 관리자 미처리 목록에 남는다. 회원 신고는 원래 자동 판단 대상이 아니었으므로 의도에 맞는 변화로 본다.
- 처리량(1시간에 3건)은 현행을 유지한다. 채팅 신고가 늘어 밀리면 별도로 조정한다.

### enum CHECK 제약을 마이그레이션으로 제거한다

#### 선택

`report_report_type_check`, `sanction_sanction_type_check` 제약을 `src/main/resources/migration/chat_report.sql`에서 `DROP CONSTRAINT IF EXISTS`로 제거한다. 새 컬럼은 `ADD COLUMN IF NOT EXISTS`로 추가하고, `is_auto_reviewed`에는 `DEFAULT FALSE`를 둔다.

#### 이유

`report`·`sanction` 테이블은 운영에서 `ddl-auto: update`로 생성돼 있는데, Hibernate가 enum 컬럼에 만든 허용값 CHECK 제약은 `ddl-auto`가 갱신하지 않아 `CHAT_MESSAGE`·`BLINDDATE_MESSAGE`·`CHAT_KICK` 같은 새 enum 값을 저장하려 하면 막힌다. `IF NOT EXISTS`는 `ddl-auto`가 먼저 컬럼을 만들어 둔 경우에도 마이그레이션이 안전하게 통과하도록 한다.

## Failure Handling

- 욕설 필터 API 호출이 실패하면 게시판 신고는 "욕설 없음"과 동일하게 처리 완료되고(기존 동작 유지), 채팅 신고는 처리하지 않고 `is_auto_reviewed`만 표시한다.
- 신고 저장 시 유니크 인덱스 위반(`DataIntegrityViolationException`)을 잡아 `DuplicateReportException`으로 변환한다. 사전에 `existsBy...` 조회로 중복을 걸러도 동시 요청 경합은 DB 제약이 최종 방어선이다.
- 자동제재 스케줄러는 신고별 처리를 비동기로 실행하고, 전체 실패나 타임아웃(60초)이 나도 스케줄러 자체는 죽지 않고 다음 주기에 다시 시도한다.

## Concurrency / Consistency

- 자동제재 스케줄러는 채팅 신고는 `CHAT_MESSAGE:messageId`, 그 외 신고는 `reportType:targetId`를 키로 쓴다. 이전 주기에서 아직 처리 중인 키(`ConcurrentHashMap` 기반 집합)와 같은 배치 안에서 이미 나온 키는 모두 건너뛰므로, 같은 메시지를 여러 사람이 신고해도 한 번에 한 건만 자동 판정한다.
- 자동 판정은 스케줄러가 넘긴 신고를 그대로 쓰지 않고 트랜잭션 안에서 다시 읽는다. 그사이 관리자가 처리했거나 삭제된 신고는 건너뛴다.
- 욕설로 판정돼도 같은 메시지에 `WARNING` 제재가 연결된 다른 신고가 이미 있으면 새 경고를 만들지 않고 제재 없이 처리 완료로 닫는다. 같은 메시지로 경고가 쌓여 자동 정지가 잘못 붙는 것을 막기 위함이다.
- 활성 제재는 회원당 여러 건 있을 수 있다고 가정한다(`SanctionRepository.findActiveSanctionsByMemberId`가 `List` 반환). 정지 우선순위 계산과 만료 처리는 애플리케이션에서 필터링·정렬한다.
- 과팅 메시지 보관소의 동시성 제약은 이 도메인이 소유하지 않는다. `blinddate` 패키지 문서가 생기면 그쪽에서 다룬다.

## 알려진 한계 / 후속 작업

- **과팅 신고 고도화(과반수 신고 시 세션 얼림)**: 사용자 신고 계획 3번 안(과반수 신고 → 세션 얼림 → 관리자 입장 대기 → 3분 초과 시 사랑의 작대기)은 이번 범위에 없다. 기존 얼리기(FREEZE/THAW)는 재사용할 수 있지만 지금은 클라이언트에 이벤트만 보내고 서버는 얼린 상태에서도 메시지를 계속 받으며, 예약된 진행 일정이 THAW를 보내 신고로 얼린 상태를 풀어버릴 수 있다. 구현하려면 서버 측 얼림 검사와 진행 일정 일시정지가 필요하다. 이번에 저장하는 과팅 신고 기록과 메시지 보관소는 그 안에서도 그대로 쓴다.
- **과팅 발신자 익명성**: 과팅 메시지 브로드캐스트 이벤트에 실제 회원 ID(`senderId`)가 그대로 노출된다.
- **정지가 로그인 시점에만 검사됨**: `ReportValidator.checkMemberAccessById`는 로그인·소셜 로그인 시점에만 호출되므로, 이미 발급된 토큰을 쓰는 회원은 정지 이후에도 만료 전까지 계속 이용할 수 있다.
- **AI 욕설 필터**: 현재 욕설 판정은 외부 텍스트 필터링 API 한 번 호출로만 이뤄진다. 더 정교한 필터는 이번 범위 밖이다.

## Related Documents

- 공통 용어 → `../glossary.md`
- 회원 간 차단 관계, 전송 차단, 게시판 목록 차단 필터 → `memberblock.md`
- 채팅방 참여·초대·추방·메시지 보관은 `chat` 패키지가 담당한다(도메인 문서 미작성)
- 과팅 진행 흐름과 메시지 보관 규칙은 `blinddate` 패키지가 담당한다(도메인 문서 미작성)

## Documentation Update Conditions

- 신고 대상 타입(`ReportType`)이나 제재 타입(`SanctionType`)이 추가·변경될 때
- 경고 누적 기준이나 자동 정지 기간이 바뀔 때
- 자동제재 스케줄러의 조회 대상이나 처리 주기가 바뀔 때
- `Report`의 저장 구조(메시지 스냅샷·맥락 컬럼)가 바뀔 때
- 과팅 신고 고도화(3번 안)가 구현돼 "알려진 한계"가 해소될 때
