# MemberBlock

> 관련 패키지: `src/main/java/com/dongsoop/dongsoop/memberblock/**`

## Responsibility

- 회원 간 차단·해제 관계를 저장하고 조회한다.
- 1:1 채팅에서 차단 관계에 따라 메시지 전송을 막고, 차단 상태를 실시간으로 알린다.
- 게시판 목록 조회 시 차단한 회원의 글을 걸러내는 필터를 제공한다.

## Out of Scope

- 신고 접수·자동 판정·관리자 제재 → `report.md`
- 채팅방 참여·메시지 저장·소켓 처리 자체 → `chat` 패키지(도메인 문서 미작성). 이 문서는 그 경로에서 차단 상태를 어떻게 계산하고 언제 전송을 막는지만 다룬다.
- 게시판 각 도메인이 필터를 어떤 쿼리로 적용하는지의 구현 상세 → 각 게시판 도메인. 이 문서는 필터가 로그인 회원 기준으로 동작한다는 규칙만 다룬다.

## Terminology

### Block (차단)

- 의미: 한 회원(차단자)이 다른 회원(피차단자)을 차단한 단방향 관계 한 건.
- 코드: `MemberBlock` 엔티티, `MemberBlockId(blocker, blockedMember)`

### BlockStatus (차단 상태)

- 의미: 특정 1:1 채팅방에서 두 참여자 사이의 차단 상태를 나타내는 값. `NONE`(차단 없음), `I_BLOCKED`(내가 상대를 차단함), `BLOCKED_BY_OTHER`(상대가 나를 차단함).
- 코드: `BlockStatus` enum
- 사용 범위: 그룹 채팅방은 항상 `NONE`을 반환한다. 그룹방에는 차단에 의한 전송 제한이 없다.

### 차단 필터 (`@ApplyBlockFilter`)

- 의미: 게시판 목록 조회 메서드에 붙이는 애너테이션. 요청 처리 동안 Hibernate `@Filter`(`blockFilter`)를 활성화해, 로그인 회원이 차단한 회원의 글을 결과에서 제외한다.
- 코드: `ApplyBlockFilter`, `MemberBlockFilterAspect`
- 사용 범위: 인증되지 않은 요청은 필터 없이 그대로 조회한다(`NotAuthenticationException` 시 필터 미적용).

## Main Flow

차단·해제:

```text
POST /member-block { blockedMemberId } (DELETE로 해제도 동일 형식)
  ↓
로그인 회원을 차단자로 사용(요청 본문 blockerId는 무시) → 자기 차단·존재하지 않는 대상 거절
  ↓
MemberBlock 저장/삭제
  ↓
두 회원이 참여한 1:1 채팅방이 있으면, 저장된 차단 관계로 각자의 BlockStatus를 다시 계산해 소켓으로 통지
```

1:1 채팅 메시지 전송 시 차단 검사:

```text
WebSocket /message/{roomId}
  ↓
getBlockStatus(roomId, senderId) — 그룹방이면 NONE, 1:1이면 상대와의 차단 관계를 그때그때 조회해 계산
  ↓
NONE이 아니면(I_BLOCKED 또는 BLOCKED_BY_OTHER) 메시지를 브로드캐스트하지 않고 조용히 무시
```

## Domain Rules

- 차단은 로그인한 회원 기준으로만 수행한다. 요청 본문의 `blockerId`는 읽지 않는다(기존 앱과의 필드 호환을 위해 필드 자체는 남겨 둔다).
- 자기 자신은 차단할 수 없다(400). 존재하지 않는 대상은 404다.
- 차단 해제 기록이 없는데 해제를 요청하면 404다.
- 1:1 채팅방에서는 `I_BLOCKED`와 `BLOCKED_BY_OTHER` 중 어느 쪽이어도 전송을 막는다. 어느 한쪽이 차단하면 양방향 모두 전송이 막힌다.
- 그룹 채팅방은 차단으로 인한 전송 제한이 없다(`getBlockStatus`가 그룹방에 대해 항상 `NONE`을 반환).
- 차단 상태는 저장하지 않고, 조회 시점에 `MemberBlock` 관계를 다시 계산해서 반환한다. 차단·해제·메시지 전송 어느 경로든 항상 같은 계산 로직(`getBlockStatus`)을 거친다.
- 게시판 목록 필터는 로그인 회원이 차단한 회원의 글만 제외한다. 인증되지 않은 요청에는 필터를 적용하지 않는다.

## Decisions

### 차단 상태를 저장하지 않고 매번 다시 계산한다

#### 선택

차단·해제 시점과 메시지 전송 시점 모두 `MemberBlock` 테이블의 관계를 조회해 `BlockStatus`를 그 자리에서 계산한다(`ChatService.getBlockStatus`). 계산된 상태를 캐시하거나 별도 컬럼에 저장하지 않는다.

#### 이유

두 회원이 서로를 차단한 경우처럼 상태가 한 값으로 고정되지 않는 경우가 있고, 차단·해제가 양쪽 모두에게 영향을 준다. 고정값을 어딘가에 저장해 두면 해제 시 그 값을 정확히 되돌려야 하는데, 관계 자체가 유일한 원본이면 계산 로직만 한 곳에 두면 된다.

#### 영향 / Trade-offs

- 메시지 전송마다 차단 관계 조회 쿼리가 추가된다. 1:1 채팅방에서만 발생하고 그룹방은 조회 없이 바로 `NONE`이라 영향은 제한적이다.
- 차단·해제 API가 소켓으로 상태를 통지할 때도 같은 계산 함수를 호출하므로, 계산 로직을 한 번만 고치면 모든 경로에 반영된다.

## Failure Handling

- 이 도메인은 별도의 비동기 처리나 재시도 대상이 없다. 차단·해제는 동기 API 호출 안에서 완료된다.
- 차단 대상 회원이 채팅방에 없거나 1:1 채팅방 자체가 없으면 소켓 통지를 건너뛰고 차단 처리만 완료한다.

## Concurrency / Consistency

- `MemberBlock`은 `(blocker, blockedMember)` 복합키라 같은 관계를 두 번 저장할 수 없다. 중복 차단 요청은 저장 전 `existsById` 조회로 막는다.
- 차단 상태는 저장된 값이 아니라 조회 시점 계산 결과이므로, 차단·해제 사이의 짧은 경합에서도 별도의 동기화 없이 항상 최신 관계를 반영한다.

## Related Documents

- 공통 용어 → `../glossary.md`
- 신고 접수와 제재는 `report.md`가 결정한다
- 채팅방 메시지 전송·소켓 처리는 `chat` 패키지가 담당한다(도메인 문서 미작성)

## Documentation Update Conditions

- `BlockStatus` 값의 의미나 계산 로직이 바뀔 때
- 차단이 1:1 채팅 외의 다른 경로(그룹방, 게시판 상호작용 등)로 확장될 때
- 게시판 차단 필터의 적용 범위나 대상이 바뀔 때
