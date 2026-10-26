# Report

> 관련 패키지: `src/main/java/com/dongsoop/dongsoop/report/**`

## Responsibility

- 게시판(프로젝트·스터디·마켓플레이스·과외) 신고, 회원 신고, 채팅 메시지 신고(일반 채팅·과팅)를 접수한다.
- 게시판 신고와 일반 채팅 신고에 대해 욕설 필터로 자동 판정하고, 판정되지 않은 신고는 관리자가 조치한다.
- 관리자 제재(경고·일시정지·영구정지·게시글 삭제·채팅방 추방)와 제재 없이 신고를 닫는 기각을 처리한다.
- 회원 본인의 제재 상태 조회를 제공한다.

## Out of Scope

- 채팅방 참여·초대·자진 퇴장과 방장의 추방 규칙 → `chat` 패키지(도메인 문서 미작성). 이 문서는 관리자가 신고 처리로 실행하는 `CHAT_KICK` 제재만 다룬다.
- 과팅 강퇴의 세션 처리(참가자 정리·퇴장 안내·재입장 차단) → `blinddate.md`. 이 문서는 과팅 신고에 `CHAT_KICK`을 허용하고 그 강퇴를 호출하는 규칙만 다룬다.
- 과팅 진행 흐름, 과팅 메시지 기록의 저장·수명, 참가 기록의 수명 → `blinddate.md`. 이 문서는 그 기록을 근거로 과팅 신고를 접수·처리하는 규칙만 다룬다.
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

### 방 종류

- 의미: 메시지 신고가 들어온 방의 종류를 신고 시점에 복사해 둔 값. 일반 채팅은 채팅 도메인의 방 종류 판단(`ChatRoom.roomType()`: 문의방 `CONTACT`, 그룹방 `GROUP`, 그 외 `ONE_TO_ONE`)을 그대로 쓰고, 과팅 신고는 `BLINDDATE`다. 게시판·회원 신고와 이 컬럼이 생기기 전 신고는 `null`이다.
- 코드: `Report.chatRoomType`, `ReportChatRoomType`, `chat_room_type` 컬럼

### 함께 해결

- 의미: 메시지 신고 한 건을 처리할 때 다른 미처리 메시지 신고를 같은 처리 주체로 처리 완료하는 것. 범위는 처리 주체에 따라 다르다. 관리자 제재·기각은 같은 신고 대상 회원(`target_member`)의 메시지 신고 전체(채팅·과팅, 메시지·방이 달라도), 자동 판정은 같은 메시지의 신고만이다. 함께 닫힌 신고는 실제로 처리된 신고의 ID를 가리키고, 제재는 연결하지 않는다. 관리자 목록에서는 그 처리된 신고의 제재 정보로 보인다.
- 코드: `ReportRepository.resolveSameTargetMessageReports`(관리자), `ReportRepository.resolveSameMessageReports`(자동 판정), `Report.resolveWith`, `Report.resolvedByReportId`

### 맥락 메시지

- 의미: 신고된 메시지 앞뒤의 사용자 메시지를 각각 최대 10개까지 모은 것. 관리자가 앞뒤 맥락을 보고 판단하도록 돕는다. 입장·퇴장 시스템 메시지는 포함하지 않는다.
  - 직전 맥락: 신고 접수 시점에 복사해 둔다.
  - 뒤쪽 맥락: 일반 채팅은 관리자 조회 시점에 원문에서 가져오고, 과팅은 세션 종료 직전에 복사해 둔다.
- 코드: `Report.messageContext`(직전), `Report.messageContextAfter`(과팅 뒤쪽 저장본), `ChatMessageSnapshots`

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
욕설 판정 → 욕설이면 자동 WARNING 생성 + 처리 완료 + 같은 메시지의 다른 미처리 신고 함께 해결
                       + 경고 누적 검사(3·5·7회 → 3·14·30일 자동 정지 + 회원의 모든 디바이스 토큰 무효화)
        → 욕설이지만 같은 메시지에 이미 경고가 있으면 새 경고 없이 처리 완료. 이 신고와 같은 메시지의 다른 미처리 신고는
          resolvedByReportId = 그 경고 신고 ID로 닫혀 목록에서 그 경고로 보인다
        → 욕설이 아니거나 API 실패면 처리하지 않고 is_auto_reviewed만 true로 표시
  ↓
관리자 목록(GET /reports/admin)에서 미처리 신고 확인(chatRoomType으로 방 종류 확인) → POST /reports/sanctions 또는 POST /reports/{id}/dismiss
  ↓
메시지 신고면 같은 대상의 다른 미처리 메시지 신고(다른 메시지·사유·과팅 포함)를 같은 관리자 명의로 함께 해결(resolvedByReportId = 처리한 신고 ID)
```

과팅 메시지 신고(자동 판정 없음). 앱이 스프링에 직접 신고하고, 스프링은 과팅 세션 메모리에 남아 있는 메시지 기록을 근거로 쓴다:

```text
POST /reports/blinddate { sessionId, messageId, reason, description }   (신고자 = 토큰의 회원)
  ↓
세션 존재 확인: 세션 메모리에 세션이 없으면 410(세션 종료·관리자 초기화·서버 재시작 모두 여기로 모인다)
  ↓
참가자 검증: 신고자의 참가 기록이 있고 그 세션이 요청 sessionId와 같아야 함(아니면 403)
  ↓
세션 메모리에서 메시지 조회(없으면 404) → 자기 신고 거절(400) → 같은 세션 내 같은 상대 중복 거절(409)
  ↓
대상 = 메시지 발신자. 메시지 스냅샷(1,000자로 자름) + 직전 맥락 메시지(최대 10개) 저장, targetUrl = /blinddate/session/{sessionId} → 201
  ↓
관리자가 GET /reports/admin에서 확인 후 POST /reports/sanctions 또는 기각으로 처리
```

과팅 신고의 뒤쪽 맥락 보존. 과팅 메시지 기록은 세션이 지워지면 사라지므로 그 직전에 복사한다:

```text
과팅 세션 종료(terminate) 또는 전체 초기화(clear) → BlindDateSessionClosedEvent(sessionId, 메시지 기록 사본)
  ↓ (비동기)
BlindDateContextAfterRecorder: BLINDDATE_MESSAGE이고 chat_room_id = sessionId이며 message_context_after가 비어 있는 신고마다
  ↓
신고된 메시지 다음 메시지 최대 10개(오래된 순)를 message_context_after에 저장. 기록에 신고 메시지가 없으면 빈 목록
  (message_context_after만 바꾸는 부분 UPDATE라 동시에 커밋된 처리 상태를 덮어쓰지 않는다)
```

관리자 맥락 조회:

```text
GET /reports/{reportId}/context   (ADMIN)
  ↓
없는 신고 404, 게시판·회원 신고 400(UnsupportedReportTypeException)
  ↓
before = 저장된 message_context
after  = CHAT_MESSAGE: 원문(Redis → chat_messages 백업)에서 신고 메시지 다음 사용자 메시지 최대 10개 → LIVE, 원문에 없으면 UNAVAILABLE
         BLINDDATE_MESSAGE: message_context_after 저장본 → SAVED, 없고 세션이 살아 있으면 세션 메모리 → LIVE, 둘 다 없으면 UNAVAILABLE
  ↓
{ messageId, messageContent, chatRoomType, before: [{senderId, content, sentAt}], after: [...], afterSource }
```

관리자 목록 응답의 메시지 신고 필드: 요약형(`UNPROCESSED`)과 상세형 모두 `chatRoomType`을 담는다. 상세형은 함께 해결된 신고에 `resolvedByReportId`(그 신고를 닫은, 실제로 처리된 신고 ID)를 담고, 직접 처리됐거나 미처리인 신고는 `null`이다.

함께 해결된 신고의 상세형 제재 필드(`sanctionType`, `sanctionReason`, `sanctionStartDate`, `sanctionEndDate`, `isSanctionActive`)는 `resolvedByReportId`가 가리키는 신고의 제재 값이다. `adminNickname`은 함께 닫을 때 복사한 처리 주체다. 처리한 신고가 기각이었거나 제재 없이 닫혔으면 제재 필드는 `null`이다. `ACTIVE_SANCTIONS` 필터도 같은 기준이라, 처리한 신고의 제재가 활성이면 함께 닫힌 신고도 포함된다. 요약형은 미처리 신고만 담으므로 함께 해결된(처리 완료) 신고가 나오지 않아 제재 필드를 두지 않는다.

정지와 토큰 무효화:

```text
SanctionExecutor가 TEMPORARY_BAN·PERMANENT_BAN 실행(관리자 제재, 경고 누적 자동 정지 모두)
  ↓
트랜잭션 안에서 회원의 디바이스 ID 목록 조회 → 커밋 후 각 디바이스를 DeviceBlacklistService.blacklist
  ↓
기존 access 토큰 → JwtFilter 401("만료된 세션입니다.") → /token/reissue도 블랙리스트 검사로 401
  ↓
디바이스 ID(did)가 없는 토큰·블랙리스트 반영 실패: access 토큰은 만료까지 쓰이지만 /token/reissue가 정지 검사(ReportValidator.checkMemberAccessById)로 401(제재 안내 문구)
  ↓
앱이 로그인 화면으로 이동 → 로그인·소셜 로그인 시 ReportValidator.checkMemberAccessById가 정지 중이면 403, 본문 sanction에 제재 내용
  ↓
정지가 끝난 뒤 로그인하면 블랙리스트 등록 시각 이후 발급된 토큰이라 통과
```

관리자 목록(`GET /reports/admin`)에는 뒤쪽 맥락을 넣지 않는다. 일반 채팅은 방 전체 메시지를 읽어야 해서 목록 한 번에 신고 수만큼 무거운 조회가 생기기 때문이다.

과팅 신고 거절 응답(검사 순서대로):

| 상황 | 응답 | 문구 |
| --- | --- | --- |
| 세션이 메모리에 없음(종료·초기화·재시작) | 410 | 종료된 과팅의 메시지는 신고할 수 없습니다. |
| 신고자가 그 세션 참가자가 아님 | 403 | 해당 과팅 세션의 참가자만 신고할 수 있습니다. |
| 진행 중 세션에 해당 메시지가 없음 | 404 | 신고할 메시지를 찾을 수 없습니다. |
| 자기 메시지 | 400 | (기존 `SelfReportException` 문구) |
| 같은 세션·같은 상대 중복 | 409 | (기존 `DuplicateReportException` 문구) |

## Domain Rules

- 채팅 메시지 신고는 시스템 메시지(입장·퇴장)를 대상으로 할 수 없고, 발신자 본인은 신고할 수 없다.
- 일반 채팅 신고는 같은 신고자·같은 메시지 조합으로 중복 신고할 수 없다(`reporter_id, message_id` 유니크).
- 과팅 신고는 같은 신고자가 같은 세션에서 같은 상대를 중복 신고할 수 없다(`reporter_id, chat_room_id, target_member_id` 유니크, `report_type = 'BLINDDATE_MESSAGE'`일 때만 적용).
- 과팅 신고는 세션이 진행 중일 때만 성립한다. 메시지 기록은 세션이 종료되면 함께 사라지므로, 세션 메모리에 세션이 없으면 참가 기록 유무와 관계없이 410으로 거절한다. 관리자 초기화는 참가 기록까지 지우지만 세션 검사를 먼저 하므로 403이 아니라 410이 된다.
- 자동제재 스케줄러는 게시판 4종과, 아직 자동 판정되지 않은(`is_auto_reviewed = false`) 일반 채팅 신고만 조회한다. 과팅 신고(`BLINDDATE_MESSAGE`)와 회원 신고(`MEMBER`)는 자동 판정 대상이 아니다.
- 경고는 영구 누적된다. 누적 3·5·7회에 도달하면 각각 3·14·30일 자동 정지가 붙는다. 8회 이상은 자동 제재 없이 관리자가 판단한다.
- 자동 정지 제재의 `reason`은 정지를 촉발한 마지막 경고(3·5·7번째) 제재의 `reason`이고, `description`은 "경고 N회 누적으로 인한 자동 D일 정지"다. 마지막 경고에 관리자가 입력한 사유가 없으면 `reason`도 그 누적 정지 문구로 대체한다. 입력 여부는 따로 저장하지 않으므로, 사유가 비어 있거나 사유 없이 발급할 때 채워지는 기본값("경고")과 같으면 입력이 없던 것으로 본다. 자동 경고의 시스템 사유("부적절한 언어 사용")는 그대로 쓴다. 자동 정지가 만드는 내부 `MEMBER` 신고의 `reportReason`은 마지막 경고 신고의 `reportReason`(없으면 `OTHER`), `description`은 같은 누적 정지 문구다.
- 정지(`TEMPORARY_BAN`, `PERMANENT_BAN`)를 발급하면 대상 회원에게 현재 연결된 모든 디바이스의 기존 토큰을 무효화한다. 정지 검사는 로그인과 토큰 재발급(`/token/reissue`) 때 한다. 무효화는 재로그인을 강제해 그 검사를 거치게 하는 수단이고, 재발급 검사는 무효화할 수 없는 토큰(디바이스 ID 없음, 블랙리스트 반영 실패)이 access 토큰 만료 뒤 이어지지 않게 한다.
- 관리자가 메시지 신고(`CHAT_MESSAGE`, `BLINDDATE_MESSAGE`)를 제재·기각하면 같은 대상 회원의 다른 미처리 메시지 신고를 채팅·과팅 구분 없이 함께 해결한다. 기각도 같다. 그래서 다른 메시지·다른 사유로 들어온 신고도 한 번의 기각으로 닫힌다. 게시판·회원 신고는 함께 해결하지 않고, 함께 해결의 대상도 아니다. 처리 이후에 새로 들어온 신고는 미처리로 들어온다.
- 자동 판정(자동 경고, 이미 경고된 메시지의 자동 처리 완료)은 같은 메시지의 미처리 신고만 함께 해결한다. 자동 판정은 그 메시지 내용만 보았으므로, 관리자가 보지 않은 다른 메시지·과팅 신고를 제재 없이 닫지 않는다. 이미 경고된 메시지로 닫힌 신고는 그 메시지의 경고 신고를 `resolvedByReportId`로 가리킨다.
- `TEMPORARY_BAN`은 관리자가 종료일을 반드시 입력해야 한다(없으면 400). `WARNING`·`PERMANENT_BAN`은 종료일 입력이 없으면 9999-12-31로 채운다(경고는 영구 누적이 정책이라 만료시키지 않기 위함). `CONTENT_DELETION`·`CHAT_KICK`은 기간 개념이 없어 시작 시각을 종료일로 쓴다.
- `CHAT_KICK`은 메시지 신고에만 허용한다(그 외 400).
  - 일반 채팅(`CHAT_MESSAGE`): 대상 방이 그룹방일 때만 추방한다. 실제 판단은 실행 시점의 방(`ChatParticipantService.kickUserByAdmin`의 `isGroupChat`)으로 하고, 신고에 저장된 `chatRoomType`은 관리자가 목록에서 미리 알 수 있게 보여 주는 값이다. 저장값의 `GROUP`·`ONE_TO_ONE`은 `isGroupChat`, `CONTACT`는 방 제목의 `[문의]` 접두사로 판단하므로, 제목이 `[문의]`로 시작하는 그룹방은 `CONTACT`로 저장돼도 추방은 허용된다. 그룹방이 아니면 400으로 거절한다. 대상이 방장이면 거절하고(방장 부재 방지), 이미 방을 나간 대상은 퇴장 시스템 메시지 없이 제재만 기록한다.
  - 과팅(`BLINDDATE_MESSAGE`): 제재가 커밋된 뒤 `BlindDateService.kickParticipant(신고의 세션 ID, 대상)`를 호출해 과팅 세션에서 강퇴한다. 세션이 이미 끝났거나 초기화됐어도 제재는 기록된다. 강퇴의 세션 처리는 `blinddate.md`가 정한다.
- 메시지 신고(`CHAT_MESSAGE`, `BLINDDATE_MESSAGE`)의 관리자 제재는 요청한 제재 대상 회원이 신고된 메시지의 작성자와 같아야 한다(다르면 400).
- 메시지 신고에는 `CONTENT_DELETION`(게시글 삭제) 제재를 쓸 수 없다(400). 삭제할 게시글이 없기 때문이다.
- 관리자 제재와 신고 기각은 이미 처리된 신고에는 쓸 수 없다(409, "이미 처리된 신고입니다. ID : n").
- 회원 본인의 제재 상태 조회(`GET /reports/sanction-status`)는 활성 상태인 정지(`TEMPORARY_BAN`, `PERMANENT_BAN`)만 노출하고, 여러 건이 활성 상태면 종료일이 가장 늦은 것을 반환한다. 경고는 영구 누적이라 노출하면 한 번 경고받은 사용자가 계속 "제재 중"으로 보이므로 노출하지 않는다.
- 정지 회원의 로그인·소셜 로그인 거절(403)은 ProblemDetail 본문의 `sanction` 필드에 제재 상태 조회와 같은 형식(`isSanctioned`, `sanctionType`, `reason`, `startDate`, `endDate`, `description`)으로 제재 내용을 담는다. 정지가 확정되면 기존 토큰이 무효화되어 제재 상태 조회도 401이 되므로, 정지 회원이 제재 내용을 받을 수 있는 곳은 로그인 응답뿐이다. 여러 건이면 종료일이 가장 늦은 정지를 담고, 영구정지는 종료일이 9999-12-31이라 항상 우선한다. 403은 다른 권한 오류에도 쓰이므로 앱은 `sanction` 필드 유무로 정지를 구분한다. 제재 상태 조회는 구 버전 앱이 앱 시작 때 호출하므로 남겨 둔다.
- 신고자에게는 처리 결과를 통보하지 않는다. 신고 접수 응답(201)만 준다.
- 게시판 신고의 판정·처리 방식(욕설 필터 호출, 실패 시 처리 완료 취급)은 이번 개편에서 바꾸지 않는다.

## Decisions

### 채팅·과팅 메시지를 신고 시점에 복사해서 저장한다

#### 선택

`Report`에 `messageContent`, `messageSentAt`, `messageContext`(직전 최대 10개 메시지) 컬럼을 두고 신고 접수 시점에 원본에서 복사한다.

#### 이유

일반 채팅 메시지는 Redis에 30일만 보관되고 이후 만료되며, 과팅 메시지는 세션 메모리에만 있고 세션이 끝나면 사라진다. 원본을 참조만 하면 관리자가 나중에 확인할 때 이미 사라져 있을 수 있어, 신고 시점의 사본을 별도로 남긴다.

#### 영향 / Trade-offs

- `Report` 테이블에 게시판 신고에서는 항상 null인 컬럼 6개가 추가된다.
- 메시지 내용이 신고 이후 원본과 별개로 고정되므로, 신고 이후 원본이 삭제·수정돼도 관리자는 신고 시점 내용을 본다.

### 과팅 신고는 세션 메모리의 메시지 기록을 근거로 받는다

#### 선택

앱이 `POST /reports/blinddate`로 세션 ID와 메시지 ID만 보내고, 스프링이 과팅 세션 메모리의 메시지 기록에서 원문·발신자·직전 맥락을 찾아 스냅샷을 만든다(`BlindDateReportTargetResolver`). 메시지 기록을 DB에 따로 남기지 않는다.

#### 이유

과팅은 스프링 STOMP로 진행되고 메시지도 스프링 메모리를 거친다. 신고 내용을 앱에서 받으면 위조할 수 있으므로, 서버가 직접 본 메시지만 근거로 삼는다. 신고되지 않은 대화까지 저장하지 않기 위해 세션 메모리 기록만 쓴다.

#### 영향 / Trade-offs

- 세션이 종료되거나 서버가 재시작되면 기록이 사라져 그 세션의 메시지는 더 이상 신고할 수 없다. 진행 중에만 신고할 수 있다.
- 신고된 메시지는 접수 시점에 `Report`로 복사되므로 세션 종료 후에도 관리자는 증거를 볼 수 있다.

### 뒤쪽 맥락은 채팅은 조회 시점에, 과팅은 세션 종료 때 확보한다

#### 선택

일반 채팅의 뒤쪽 맥락은 저장하지 않고 `GET /reports/{reportId}/context` 호출 때 원문에서 읽는다. 과팅은 세션 종료·전체 초기화 때 과팅 도메인이 발행하는 `BlindDateSessionClosedEvent`를 받아 `message_context_after`에 저장한다.

#### 이유

신고 접수 시점에는 뒤쪽 대화가 아직 없다. 일반 채팅 원문은 Redis(30일)와 `chat_messages` 백업에 남으므로 조회 때 읽으면 된다. 과팅 기록은 세션 메모리에만 있어 세션이 지워지면 다시 얻을 수 없으므로 지워지기 직전에 복사해야 한다. 과팅 도메인이 신고를 알지 않도록 이벤트로 연결한다.

#### 영향 / Trade-offs

- 리스너는 기본 `@Async` 실행기(Spring Boot 자동 실행기가 다른 실행기 빈 때문에 꺼져 있어 작업마다 새 스레드)에서 돈다. `autoSanctionExecutor`는 큐가 10이라 거절 시 예외가 과팅 종료 스레드로 올라오므로 쓰지 않는다. 리스너 예외는 Spring 기본 비동기 예외 처리기가 로그로만 남긴다.
- 세션 종료와 신고 저장 커밋이 겹치면(신고 검증은 통과했지만 커밋 전에 세션이 지워짐) 리스너가 그 신고를 못 보고 지나가 뒤쪽 맥락이 `UNAVAILABLE`로 남을 수 있다.
- 서버 재시작으로 메모리가 사라질 때는 이벤트가 발행되지 않아 뒤쪽 맥락이 저장되지 않는다.
- 일반 채팅 원문이 만료·삭제되면 뒤쪽 맥락은 `UNAVAILABLE`이 된다.

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

### 방 종류는 신고 시점에 스냅샷한다

#### 선택

`report.chat_room_type`에 신고 시점의 방 종류를 저장한다(`ChatReportTargetResolver`는 `ChatRoom.roomType()`, `BlindDateReportTargetResolver`는 `BLINDDATE`). 채팅 목록 응답의 방 종류 판단(`ChatService.determineRoomType`)도 같은 `ChatRoom.roomType()`을 쓰도록 옮겨, 판단 로직은 채팅 도메인 한 곳에만 둔다.

#### 이유

채팅방은 마지막 참여자가 나가면 삭제되고, 과팅 세션은 끝나면 메모리에서 사라진다. 관리자 조회 시점에 방을 다시 읽으면 이미 없을 수 있어 신고 시점 값을 남긴다. 채팅 도메인의 `ChatRoomType`(값 `CONTACT`·`GROUP`·`ONE_TO_ONE`)에는 과팅이 없어 신고 도메인이 `BLINDDATE`를 더한 `ReportChatRoomType`을 소유하되, 채팅 쪽 이름은 그대로 쓴다.

#### 영향 / Trade-offs

- 기존 신고 행은 `null`이다. 마이그레이션(`src/main/resources/migration/648_report_followups.sql`)은 `chat_room_type`·`resolved_by_report_id` 컬럼을 `ADD COLUMN IF NOT EXISTS`로 추가하고 과거 값을 채우지 않는다. `ddl-auto`가 먼저 만든 `report_chat_room_type_check` 제약은 같은 파일에서 제거한다.
- 신고 이후 방 종류가 바뀌어도(현재 바뀌는 경로는 없다) 저장값은 신고 시점 그대로다. `CHAT_KICK` 실행 검증은 저장값이 아니라 실행 시점의 방으로 한다.

### 나머지 메시지 신고는 `resolved_by_report_id`로 함께 닫는다

#### 선택

관리자가 메시지 신고를 처리한 트랜잭션 안에서 같은 대상 회원의 다른 미처리 메시지 신고를 `SELECT ... FOR UPDATE`(ID 순)로 읽어, 처리 완료·같은 관리자·`resolved_by_report_id = 처리한 신고 ID`로 바꾼다(`ReportRepository.resolveSameTargetMessageReports`). 자동 판정은 같은 메시지의 미처리 신고만 같은 방식으로 닫는다(`ReportRepository.resolveSameMessageReports`). 자동 경고면 처리한 신고를, 이미 경고된 메시지면 그 메시지의 기존 경고 신고(가장 먼저 경고된 것)를 가리킨다. 관리자 목록은 `resolved_by_report_id`로 처리한 신고를 조인해 그 제재를 함께 닫힌 신고의 제재로 보여 준다(`ReportRepositoryCustomImpl.createBaseQuery`).

같은 대상의 신고를 처리하는 모든 경로(관리자 제재·기각, 자동 판정의 자동 경고·이미 경고됨)는 처리 시작 때 대상 회원 ID로 `pg_advisory_xact_lock`을 잡은 뒤 신고 행을 잠근다(`ReportRepository.findByIdForUpdateLockingTarget`). 트랜잭션이 끝나면 잠금이 풀린다.

#### 이유

같은 대상에게 신고가 여러 건이면 관리자가 하나를 처리했을 때 나머지도 같은 제재 상태로 보여야 한다. 자동 판정은 욕설 필터로 그 메시지만 판단하므로, 대상 전체를 닫으면 관리자가 보지 못한 다른 사유(스팸·사기 등)·과팅 신고가 제재 없이 닫힌다. 그래서 자동 판정의 범위는 같은 메시지로 제한한다. 함께 닫힌 신고에 같은 제재를 연결하면 `countActiveWarningsForMember`가 신고 행 기준으로 경고를 세므로 경고 한 건이 신고 수만큼 누적돼 자동 정지가 잘못 붙는다. 그래서 제재 행은 처리한 신고에만 두고, 함께 닫힌 신고는 그 신고를 가리키는 ID 하나만 갖고 조회 때 조인한다. 처리 시각 컬럼은 따로 없으며, 같은 트랜잭션에서 갱신되므로 `updated_at`이 처리한 신고와 같은 시점이 된다.

대상 단위로 직렬화하는 이유: 신고 행만 잠그면 두 처리가 각자 자기 신고를 잠근 뒤 함께 해결에서 상대 행을 기다려 교착된다. 대상 잠금을 먼저 잡으면 늦게 온 처리는 대상 잠금에서 기다리다가, 앞 처리가 커밋한 뒤 자기 신고가 이미 처리된 것을 보고 409(관리자) 또는 건너뛰기(자동 판정)로 끝난다. 그래서 두 처리가 서로를 건너뛰어 제재가 두 건 생기는 일이 없다. 대상 ID는 엔티티가 아닌 스칼라 조회로 먼저 읽는다. 엔티티를 먼저 읽어 두면 뒤의 잠금 조회가 영속성 컨텍스트에 있던 이전 상태를 돌려줘 처리 여부 재확인이 무력해진다.

#### 영향 / Trade-offs

- 기각도 함께 해결하므로, 다른 메시지·다른 사유로 들어온 같은 대상 신고까지 한 번의 기각으로 닫힌다. 사용자 요청("같은 대상의 신고는 하나가 처리되면 나머지도 같은 상태")을 따른 결정이다.
- 자동 판정의 "이미 경고된 메시지"로 닫힌 신고는 새 제재 없이 기존 경고 신고를 가리키므로 목록에서 그 경고로 보인다. 경고 신고의 `admin`이 함께 복사된다.
- 처리 이후 새로 들어온 같은 대상 신고는 미처리로 남는다. 일반 채팅은 자동 판정에서 "이미 경고된 메시지"로 닫힐 수 있다.
- 자동 판정 뒤에도 같은 대상의 다른 메시지·과팅 신고는 미처리로 남아 관리자가 따로 처리한다.
- advisory lock 키는 대상 회원 ID 하나를 그대로 쓴다. 이 저장소에서 advisory lock을 쓰는 곳은 여기뿐이다. 다른 곳에서 쓰게 되면 키가 겹치지 않게 두 인자 형식으로 구분해야 한다.
- Postgres 전용 쿼리다. 신고 처리 경로를 H2에서 실행하는 테스트는 없다(처리 경로 통합 테스트는 Testcontainers Postgres).

### 정지 시 기존 토큰은 디바이스 블랙리스트로 무효화한다

#### 선택

`SanctionExecutor`가 정지를 실행하면 회원의 디바이스 ID를 트랜잭션 안에서 읽고, 커밋 후(`TransactionSynchronization.afterCommit`) 로그아웃·강제 로그아웃이 쓰는 `DeviceBlacklistService.blacklist`로 등록한다. 블랙리스트 반영이 실패하면 로그만 남기고 제재는 유지한다. STOMP CONNECT(`StompHandler`)도 HTTP 요청과 같이 블랙리스트를 검사하므로 새 연결만 막는다. 토큰 재발급(`JwtServiceImpl.issuedTokenByRefreshToken`)은 블랙리스트와 별도로 로그인과 같은 정지 검사(`ReportValidator.checkMemberAccessById`)를 한다.

#### 이유

새 강제 로그아웃 수단을 만들지 않고, 이미 HTTP 요청(`JwtFilter`)과 토큰 재발급(`JwtServiceImpl`)이 검사하는 블랙리스트를 재사용한다. 블랙리스트는 등록 시각 이전에 발급된 토큰만 거부하고 TTL이 리프레시 토큰 만료 시간이라, 정지가 끝난 뒤 새로 로그인해 받은 토큰은 막지 않는다. 커밋 후에 반영해야 롤백된 제재로 로그아웃되지 않는다.

#### 영향 / Trade-offs

- 토큰에 디바이스 ID(`did`)가 없으면(로그인 때 FCM 토큰이 없거나 등록되지 않은 디바이스, 블랙리스트 도입 전 토큰) 블랙리스트로 무효화할 수 없다. 이 access 토큰은 만료 전까지 쓰이지만, 만료 뒤 재발급이 정지 검사로 401이 되어 더 이어지지 않는다. 앱은 재발급이 401일 때만 토큰을 지우고 세션 만료로 처리하므로, 로그인 거절(403)과 달리 401로 응답하고 문구는 제재 안내를 그대로 쓴다(`SanctionedMemberReissueException`). 블랙리스트 반영이 실패한 경우도 같다.
- 무효화 대상은 정지 시점에 그 회원에게 연결된 디바이스다. 다른 회원으로 다시 연결된 디바이스에서 예전에 발급된 토큰은 대상이 아니다.
- 이미 열린 STOMP 연결(채팅·과팅)은 CONNECT 때만 인증하므로 끊기지 않는다. 서버가 연결을 끊으려면 WebSocket 세션 참조를 보관하는 장치가 필요한데 현재 `WebSocketSessionManager`는 회원당 세션 ID 하나만 들고 있어 닫을 수 없다. 끊긴 뒤 재연결은 블랙리스트 검사로 거절된다.

### enum CHECK 제약을 마이그레이션으로 제거한다

#### 선택

`report_report_type_check`, `sanction_sanction_type_check` 제약을 `src/main/resources/migration/417_chat_report.sql`에서, `report_chat_room_type_check` 제약을 `648_report_followups.sql`에서 `DROP CONSTRAINT IF EXISTS`로 제거한다. 새 컬럼은 `ADD COLUMN IF NOT EXISTS`로 추가하고, `is_auto_reviewed`에는 `DEFAULT FALSE`를 둔다.

#### 이유

`report`·`sanction` 테이블은 운영에서 `ddl-auto: update`로 생성돼 있는데, Hibernate가 enum 컬럼에 만든 허용값 CHECK 제약은 `ddl-auto`가 갱신하지 않아 `CHAT_MESSAGE`·`BLINDDATE_MESSAGE`·`CHAT_KICK` 같은 새 enum 값을 저장하려 하면 막힌다. `IF NOT EXISTS`는 `ddl-auto`가 먼저 컬럼을 만들어 둔 경우에도 마이그레이션이 안전하게 통과하도록 한다.

## Failure Handling

- 욕설 필터 API 호출이 실패하면 게시판 신고는 "욕설 없음"과 동일하게 처리 완료되고(기존 동작 유지), 채팅 신고는 처리하지 않고 `is_auto_reviewed`만 표시한다. 두 경우 모두 해당 신고가 스케줄러 조회 대상에서 빠지므로, 필터 장애가 같은 신고를 매 주기 다시 집어 대기열을 막지 않는다.
- 필터 호출에는 연결 3초·읽기 5초 타임아웃이 있다(`TextFilteringService`). 타임아웃도 호출 실패와 같이 취급한다. 타임아웃이 없으면 필터 장애 시 자동제재 스레드가 무기한 묶인다.
- 신고 접수는 게시판·채팅·과팅 모두 `ReportService`가 받는다. 메시지 신고 저장 시 유니크 인덱스 위반(`DataIntegrityViolationException`)을 잡아 `DuplicateReportException`으로 변환한다. 사전에 `existsBy...` 조회로 중복을 걸러도 동시 요청 경합은 DB 제약이 최종 방어선이다.
- 자동제재 스케줄러는 신고별 처리를 비동기로 실행하고, 전체 실패나 타임아웃(60초)이 나도 스케줄러 자체는 죽지 않고 다음 주기에 다시 시도한다. 처리 중 키는 처리가 성공·실패 어느 쪽으로 끝나도 지우고, 실행기 큐가 차서 제출이 거절된 경우에도 지운다.

## Concurrency / Consistency

- 자동제재 스케줄러는 채팅 신고는 `CHAT_MESSAGE:messageId`, 그 외 신고는 `reportType:targetId`를 키로 쓴다. 이전 주기에서 아직 처리 중인 키(`ConcurrentHashMap` 기반 집합)와 같은 배치 안에서 이미 나온 키는 모두 건너뛰므로, 같은 메시지를 여러 사람이 신고해도 한 번에 한 건만 자동 판정한다.
- 관리자 제재·기각과 자동 판정의 결과 반영은 모두 대상 회원 advisory lock을 잡고 신고 행을 비관적 쓰기 잠금으로 읽은 뒤(`ReportRepository.findByIdForUpdateLockingTarget`) 처리 여부를 검사한다. 같은 대상의 신고 처리는 한 번에 하나씩 진행되고, 먼저 잡은 쪽이 처리와 함께 해결을 끝내고 커밋하면 뒤의 쪽은 처리된 신고를 보고 409(관리자) 또는 건너뛰기(자동 판정)로 끝난다.
- 자동 판정은 욕설 필터 HTTP 호출을 트랜잭션 밖에서 한다. 호출 전에 잠금 없이 한 번 읽어 이미 처리된 신고는 건너뛰고, 필터 결과를 받은 뒤 새 트랜잭션에서 잠금 조회로 다시 읽어 미처리일 때만 제재·처리 완료·`is_auto_reviewed` 표시를 한다. 필터 대기(최대 약 8초) 동안 DB 커넥션과 행 잠금을 잡지 않고, 그사이 관리자가 기각한 신고에 자동 경고가 덧씌워지지 않는다.
- 함께 해결은 대상 잠금을 잡은 같은 트랜잭션에서 미처리 메시지 신고(관리자는 같은 대상, 자동 판정은 같은 메시지)를 ID 순 `FOR UPDATE`로 잠그고 갱신한다. 같은 대상의 다른 처리는 대상 잠금에서 기다리므로 행 잠금끼리 교착되지 않는다.
- 정지에 따른 디바이스 블랙리스트 등록과 과팅 강퇴는 제재 트랜잭션 커밋 후에 실행한다. 트랜잭션 밖에서 호출되면(테스트 등) 즉시 실행한다.
- 욕설로 판정돼도 같은 메시지에 `WARNING` 제재가 연결된 다른 신고가 이미 있으면 새 경고를 만들지 않고 제재 없이 처리 완료로 닫는다. 같은 메시지로 경고가 쌓여 자동 정지가 잘못 붙는 것을 막기 위함이다.
- 모든 제재(관리자 제재, 자동 게시글 삭제·경고, 누적 자동 정지)는 `SanctionExecutor.issue` 한 곳에서 만들고 저장한다. 제재 행을 먼저 flush한 뒤 효과(게시글 삭제·채팅방 추방·경고 누적 검사)를 실행해, DB 제약 오류가 되돌릴 수 없는 Redis 추방보다 먼저 드러나게 한다.
- 과팅 뒤쪽 맥락 저장은 `message_context_after`만 바꾸는 부분 UPDATE(`ReportRepository.recordContextAfter`, `WHERE message_context_after IS NULL`)다. 엔티티를 flush하면 모든 컬럼을 덮어써, 그사이 커밋된 관리자 처리·함께 해결의 `is_processed`·`sanction_id`를 되돌릴 수 있기 때문이다. 이미 값이 있으면 DB 조건으로 건너뛴다. 대상 잠금을 거치지 않으므로 신고마다 따로 커밋해 행 잠금을 한 번에 하나만 잡는다. 한 트랜잭션에서 여러 행을 잡으면 관리자 처리(처리한 신고를 먼저 잠근 뒤 나머지를 ID 순으로 잠금)와 서로의 행을 기다려 교착될 수 있다.
- 활성 제재는 회원당 여러 건 있을 수 있다고 가정한다(`SanctionRepository.findActiveSanctionsByMemberId`가 `List` 반환). 정지 우선순위 계산과 만료 처리는 애플리케이션에서 필터링·정렬한다.

## 알려진 한계 / 후속 작업

- **과팅 신고 고도화(과반수 신고 시 세션 얼림)**: 사용자 신고 계획 3번 안(과반수 신고 → 세션 얼림 → 관리자 입장 대기 → 3분 초과 시 사랑의 작대기)은 이번 범위에 없다. 기존 얼리기(FREEZE/THAW)는 재사용할 수 있지만 지금은 클라이언트에 이벤트만 보내고 서버는 얼린 상태에서도 메시지를 계속 받으며, 예약된 진행 일정이 THAW를 보내 신고로 얼린 상태를 풀어버릴 수 있다. 구현하려면 서버 측 얼림 검사와 진행 일정 일시정지가 필요하다. 이번에 저장하는 과팅 신고 기록은 그 안에서도 그대로 쓴다.
- **정지 후에도 남는 접근**: 디바이스 ID가 없는 access 토큰은 만료까지(재발급은 막힌다), 이미 열린 STOMP 연결은 끊길 때까지 남는다(위 "정지 시 기존 토큰은 디바이스 블랙리스트로 무효화한다" 참고).
- **AI 욕설 필터**: 현재 욕설 판정은 외부 텍스트 필터링 API 한 번 호출로만 이뤄진다. 더 정교한 필터는 이번 범위 밖이다.

## Related Documents

- 공통 용어 → `../glossary.md`
- 회원 간 차단 관계, 전송 차단, 게시판 목록 차단 필터 → `memberblock.md`
- 채팅방 참여·초대·추방·메시지 보관은 `chat` 패키지가 담당한다(도메인 문서 미작성)
- 과팅 진행 흐름과 메시지 기록 → `blinddate.md`

## Documentation Update Conditions

- 신고 대상 타입(`ReportType`)이나 제재 타입(`SanctionType`)이 추가·변경될 때
- 경고 누적 기준이나 자동 정지 기간이 바뀔 때
- 자동제재 스케줄러의 조회 대상이나 처리 주기가 바뀔 때
- `Report`의 저장 구조(메시지 스냅샷·맥락·방 종류·함께 해결 컬럼)가 바뀔 때
- 정지 시 토큰 무효화 방식, 함께 해결 규칙, 신고 처리 직렬화 방식, 과팅 강퇴 호출 방식이 바뀔 때
- 신고 맥락 조회 API의 응답이나 뒤쪽 맥락 확보 방식이 바뀔 때
- 과팅 신고 고도화(3번 안)가 구현돼 "알려진 한계"가 해소될 때
