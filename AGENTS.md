# AGENTS.md

동숲(Dongsoop) 백엔드. Java 17 / Spring Boot 3.4.3 / Gradle / PostgreSQL / Redis / Elasticsearch.
모든 코딩 에이전트(Claude, Codex, Gemini 등)는 이 파일을 규칙의 단일 출처로 삼는다.

## 작업 흐름 (필수)

1. **먼저 issue를 만든다.** `.github/ISSUE_TEMPLATE` 의 `[FEAT]` / `[BUG]` 템플릿을 사용한다.
2. `main` 에서 브랜치를 딴다. 이름은 `feat/` · `fix/` · `refactor/` · `test/` · `chore/` · `docs/` + 케밥케이스.
3. **`main` 에 직접 push 하지 않는다.** 변경은 반드시 PR 로 올린다.
4. PR 본문은 `.github/pull_request_template.md` 를 따르고, 관련 이슈를 `Closes #번호` 로 연결한다.

## 명령어

```bash
./gradlew test    # 테스트 (PR CI 가 main/dev 대상으로 동일하게 실행)
./gradlew build   # 빌드
```

## 컨벤션

- 커밋 메시지: `<type>: <한국어 요약>` (예: `fix: 이클래스 과제 목록의 submitted 가 항상 false 이던 문제`). type 은 브랜치 접두사와 같은 집합을 쓴다.
- 패키지 구조는 도메인별로 나누고 그 안에서 `controller` / `service` / `repository` / `entity` / `dto` 로 나눈다.
- DB 스키마 변경은 `src/main/resources/migration/` 에 번호를 붙인 SQL 로 추가한다.
- 정적 분석은 SonarQube(`.github/workflows/sonar.yml`)가 PR 에서 돌아간다.

## 에이전트 컨텍스트 파일

- 규칙은 이 파일에 쓴다. Claude 전용 동작에만 해당하는 내용이 아니면 `CLAUDE.md` 에 쓰지 않는다.
- `CLAUDE.md` 는 `@AGENTS.md` 한 줄로 이 파일을 가리키며, 그 아래에는 Claude 종속적인 내용만 덧붙인다.
- 커밋 메시지와 PR 본문에 에이전트 흔적(`Co-Authored-By: Claude`, `Generated with ...` 등)을 남기지 않는다.
