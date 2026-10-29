# Java 포맷

Spotless 8.2.1에서 Google Java Format 1.28.0의 Google 스타일(들여쓰기 2칸)을 사용한다. 포매터 버전은 Java 17에서 실행할 수 있는 버전으로 고정했다. main/test 소스 전체를 검사하며 build 및 generated 디렉터리는 제외한다. 변경 파일만 검사하거나 기존 위반을 유예하는 baseline은 사용하지 않는다.

```bash
./gradlew spotlessApply
./gradlew spotlessCheck
```

`spotlessApply`로 수정한 diff를 확인한 후 필요한 파일만 stage한다. `check`/`build`에도 Spotless 검사가 연결된다. 포매터는 스타일을 통일하며 Sonar의 코드 스멜 분석을 대체하지 않는다.

## PR과 병합

Format workflow의 `Java format` job은 경로·대상 브랜치 필터 없이 모든 PR과 merge queue에서 실행한다. main/dev의 기존 보호 규칙에는 이 상태 검사를 필수로 추가한다. 문서만 바뀐 PR도 검사한다. 실패하면 `spotlessApply` 후 커밋한다.

기존 Java 파일을 한 번에 정리했으므로 진행 중인 다른 브랜치는 이 변경을 반영한 후 포맷을 다시 실행해야 할 수 있다.

## Git 훅

```bash
bash scripts/install-hooks.sh
```

clone마다 한 번 설치한다. Git은 versioned 훅을 clone만으로 활성화하지 않는다. 설치기는 기존 `core.hooksPath` 또는 실행 가능한 pre-commit/pre-push 훅을 덮어쓰지 않고 중단한다. 기존 관리자가 있으면 `bash scripts/check-format.sh`를 기존 훅에서 호출한다.

pre-commit은 검사할 Java/Gradle 파일의 작업 트리와 index가 다르면 중단한다. 포매터가 작업 트리만 읽기 때문에 stage된 위반을 stage하지 않은 수정으로 숨기는 상황을 방지한다. 부분 stage 작업은 필요한 수정과 포맷을 함께 정리한 뒤 다시 시도한다.

pre-push는 Java/Gradle 파일이 HEAD와 같을 때 검사한다. 커밋하지 않은 수정으로 잘못된 커밋의 포맷 검사를 통과시키지 않는다. 두 훅은 소스를 자동 수정하거나 `git add`하지 않는다. 이 훅을 끄더라도 PR의 필수 검사는 유지된다.

로컬 설정 해제: `git config --local --unset core.hooksPath` (이 저장소에서 `.githooks`를 설치한 경우에만 실행).

## 에이전트 하네스

`.codex/hooks.json`과 `.claude/settings.json`의 Stop 훅은 같은 `scripts/format-stop-hook.py`를 호출한다. Java/Gradle 변경이 남았을 때만 `scripts/check-format.sh`를 실행한다. 성공 시 `{}`를 출력하고, 실패 시 `decision: block`과 수정 안내를 반환한다. 출력 로그는 stderr로 보내 JSON 응답과 섞이지 않는다.

무한 재시도를 막기 위해 `stop_hook_active`인 재호출에는 다시 종료를 막지 않는다. 이는 한 번의 조기 피드백이며 최종 병합 차단은 PR 필수 검사가 담당한다. 코드 변경이 없는 질문에는 Gradle을 실행하지 않는다. JDK 17과 Python 3가 필요하며, 첫 실행은 Gradle 의존성을 내려받을 수 있다.

Codex는 프로젝트와 훅 정의에 대한 신뢰가 필요하다. `/hooks`에서 설정을 검토하고 활성화한다. 훅 신뢰를 우회하는 설정은 추가하지 않는다. Claude Code도 프로젝트 설정을 검토하고 세션에서 훅이 로드됐는지 확인한다. 저장소의 Git 훅은 두 하네스 외의 에이전트에도 공통 적용된다.

공식 문서:

- https://github.com/diffplug/spotless/tree/main/plugin-gradle
- https://github.com/google/google-java-format
- https://learn.chatgpt.com/docs/hooks
- https://code.claude.com/docs/en/hooks
