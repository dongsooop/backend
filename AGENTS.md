# AGENTS.md

동숲(Dongsoop) 백엔드. Java 17 / Spring Boot 3.4.3 / Gradle / PostgreSQL / Redis / Elasticsearch.
모든 코딩 에이전트(Claude, Codex, Gemini 등)는 이 파일을 규칙의 단일 출처로 삼는다.

## 작업 흐름 (필수)

1. **먼저 Issue를 만든다.** `.github/ISSUE_TEMPLATE`의 템플릿을 사용한다.
2. Issue의 **정의 / 기대 효과 / 체크포인트 / 범위**를 구현 전에 확인한다.
3. `main`에서 브랜치를 만든다. 브랜치 이름은 `{type}/{issue-number}` 형식을 사용한다.
   - 예: `feat/101`, `fix/102`, `refactor/103`, `test/104`, `chore/105`, `docs/106`
4. 해당 도메인을 수정하기 전에 `docs/domains/<domain>.md`가 있는지 확인하고, 존재하면 반드시 읽는다.
5. 문서와 코드에서 사용하는 용어는 아래 **용어 소유 규칙**을 따른다.
6. **`main`에 직접 push 하지 않는다.** 변경은 반드시 PR로 올린다.
7. PR 본문의 첫 줄은 반드시 `Closes #이슈번호`로 시작한다.
8. PR 본문은 `.github/pull_request_template.md`를 따른다.

## Issue 체크포인트

Issue의 체크포인트는 작업의 원래 완료 조건이다.

- PR의 **이슈 기준** 섹션에 그대로 옮긴다.
- 구현 과정에서 임의로 삭제하거나 의미를 바꾸지 않는다.
- 요구사항 자체가 바뀌었다면 Issue를 먼저 수정한다.
- 구현 중 새롭게 확인된 필수 조건은 PR의 **구현 중 추가** 섹션에 분리한다.
- 현재 Issue의 목적과 독립적인 개선은 현재 PR에 끼워 넣지 않고 별도 Issue로 만든다.

## 문서 구조

### 공통 용어

`docs/glossary.md`는 **여러 도메인에 걸쳐 공통으로 사용하는 용어**만 관리한다.

- 같은 개념에 여러 이름을 사용하지 않는다.
- 프로젝트 전반에서 반복되는 개념만 Glossary에 둔다.
- 특정 도메인에 속한 용어는 해당 도메인 문서가 소유한다.
- 기존 공통 용어의 의미를 변경하면 영향을 받는 도메인 문서를 함께 검토한다.

### 도메인 용어

각 도메인은 자신의 `docs/domains/<domain>.md`의 **Terminology** 섹션에서 해당 도메인이 소유한 용어를 정의한다.

다른 도메인이 그 개념을 사용해야 한다면 새로운 이름을 만들지 않고 **소유 도메인에서 정의한 용어를 그대로 사용한다.**

예:

- Notice 도메인이 `Notice`라는 용어를 정의했다면 Notification 도메인에서도 같은 개념을 `Announcement`, `Board Post` 등으로 바꾸어 부르지 않는다.
- 다른 도메인의 용어 설명을 현재 문서에 복사하지 않고 해당 도메인 문서를 참조한다.

용어 역시 다음 원칙을 따른다.

- One concept, one term.
- One term, one owner.
- 연결된 도메인은 소유 도메인의 용어를 그대로 사용한다.

### 도메인 문서

도메인 문서의 단일 원본은 `docs/domains/<domain>.md`이다.

예:

- `src/main/java/com/dongsoop/dongsoop/notice/**` → `docs/domains/notice.md`
- `src/main/java/com/dongsoop/dongsoop/member/**` → `docs/domains/member.md`

도메인 패키지 내부에 동일 내용을 복제한 장문 README를 만들지 않는다.

도메인 문서는 다음을 설명한다.

- 도메인의 책임
- 책임지지 않는 범위
- 도메인이 소유한 용어
- 주요 흐름
- 도메인 규칙
- 중요한 구현 원리
- 현재 방식을 선택한 이유
- 주요 trade-off와 영향
- 실패 처리나 동시성 등 수정 시 중요한 제약
- 관련 문서

### 문서 책임 경계

각 문서는 자신이 소유한 결정만 상세히 설명한다.

다른 도메인이 결정하는 정책을 현재 문서에 다시 정의하지 않는다.

예:

> 신규 공지가 저장된 이후 알림 생성이 발생할 수 있다.
> 알림 대상 선정과 발송 정책은 `docs/domains/notification.md`에서 결정한다.

다음 원칙을 지킨다.

- One concept, one term.
- One decision, one owner.
- One policy, one source of truth.
- 다른 문서가 소유한 결정과 용어 정의는 복사하지 않고 참조한다.
- 현재 문서의 책임이 끝나는 지점을 명확하게 적는다.
- 참조 대상의 세부 정책이 바뀌어도 현재 문서를 불필요하게 수정하지 않도록 작성한다.

### 문서 갱신 기준

다음 변경이 발생하면 관련 도메인 문서를 검토한다.

- 도메인 규칙 변경
- 도메인이 소유한 용어의 의미 변경
- 주요 처리 흐름 변경
- 저장 구조 변경
- 외부 시스템 연동 방식 변경
- Scheduler / Queue / Event 처리 변경
- 트랜잭션 경계 변경
- 중요한 예외 처리 정책 변경
- 동시성 제어 방식 변경
- 해당 구현 방식을 선택한 이유나 trade-off가 달라진 경우

단순 이름 변경, 포맷팅, 내부 리팩터링처럼 도메인 동작과 설계 의도가 변하지 않는 경우 불필요하게 문서를 수정하지 않는다.

### 문서에 기록하지 않는 정보

저장소는 Public이므로 다음 정보는 문서에 기록하지 않는다.

- Secret / Credential / API Key
- 실제 사용자 개인정보
- 내부 운영 계정
- 비공개 인프라 접근 정보
- 보안상 외부 공개하면 안 되는 값이나 절차

## PR 작성 원칙

PR은 작업 로그가 아니라 **최종 변경의 설명**이다.

구현 과정의 모든 시행착오를 시간순으로 기록하지 않는다.

예를 들어 구현이 `A → B → A` 순서로 바뀌었다면 전체 과정을 나열하지 않는다.
최종적으로 A를 선택한 이유가 중요하다면 다음처럼 작성한다.

> B 방식은 특정 조건에서 중복 처리가 발생할 수 있어 A 방식을 선택했다.

PR에는 다음을 담는다.

- 최종적으로 무엇을 변경했는가
- Issue 체크포인트를 어떻게 충족했는가
- 구현 중 새롭게 추가된 필수 조건이 무엇인가
- 어떤 원리와 흐름으로 구현했는가
- 왜 이 방식을 선택했는가
- 어떤 영역에 영향이 있는가
- 어떻게 검증했는가
- 어떤 문서를 확인하거나 갱신했는가
- 후속 작업이 있다면 어떤 Issue로 분리했는가

## 명령어

```bash
./gradlew test
./gradlew build
```

## 컨벤션

- 커밋 메시지: `<type>: <한국어 요약>`
- type은 `feat`, `fix`, `refactor`, `test`, `chore`, `docs` 등을 사용한다.
- 패키지 구조는 도메인별로 나누고 그 안에서 `controller` / `service` / `repository` / `entity` / `dto`로 나눈다.
- DB 스키마 변경은 `src/main/resources/migration/`에 번호를 붙인 SQL로 추가한다.
- 정적 분석은 SonarQube(`.github/workflows/sonar.yml`)가 PR에서 돌아간다.

## 에이전트 컨텍스트 파일

- 규칙은 이 파일에 쓴다. Claude 전용 동작에만 해당하는 내용이 아니면 `CLAUDE.md`에 쓰지 않는다.
- `CLAUDE.md`는 `@AGENTS.md` 한 줄로 이 파일을 가리키며, 그 아래에는 Claude 종속적인 내용만 덧붙인다.
- 커밋 메시지와 PR 본문에 에이전트 흔적(`Co-Authored-By: Claude`, `Generated with ...` 등)을 남기지 않는다.
