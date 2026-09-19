# <Domain>

> 관련 패키지: `src/main/java/com/dongsoop/dongsoop/<domain>/**`

## Responsibility

이 문서와 도메인이 책임지는 범위를 작성합니다.

- 
- 

## Out of Scope

이 문서에서 결정하지 않는 범위를 작성합니다.

- 
- 

다른 도메인이 소유하는 결정이라면 해당 문서를 연결합니다.

예:

- 알림 대상 및 발송 정책 → `notification.md`

## Terminology

공통 용어는 `../glossary.md`를 따릅니다.

이 도메인에서만 사용하는 용어가 있다면 여기에서 정의합니다.

## Main Flow

주요 처리 흐름을 구현 클래스 나열이 아니라 책임과 데이터 흐름 중심으로 설명합니다.

```text
Input
  ↓
Domain Operation
  ↓
Persistence / External System
  ↓
Result
```

## Domain Rules

반드시 유지되어야 하는 도메인 규칙을 작성합니다.

- 
- 

## Decisions

### <Decision>

#### 선택

현재 사용하는 방식을 설명합니다.

#### 이유

왜 이 방식을 선택했는지 설명합니다.

과거의 모든 시행착오를 기록하지 않고, 현재 선택이 의미 있는 다른 선택지보다 나은 이유만 남깁니다.

#### 영향 / Trade-offs

이 선택으로 얻는 것과 감수하는 것을 작성합니다.

## Failure Handling

실패, 재시도, 부분 실패, 중복 처리 등 중요한 정책이 있다면 작성합니다.

해당 정책이 다른 도메인이 소유한 결정이면 상세히 적지 말고 그 문서를 참조합니다.

## Concurrency / Consistency

동시성, 트랜잭션, 멱등성, 정합성 관련 중요한 제약이 있다면 작성합니다.

## Related Documents

- 공통 용어 → `../glossary.md`
- 

## Documentation Update Conditions

다음 변경이 발생하면 이 문서를 검토합니다.

- 도메인 규칙 변경
- 주요 처리 흐름 변경
- 저장 구조 변경
- 실패 처리 또는 동시성 정책 변경
- 주요 기술적 결정과 그 이유 변경
