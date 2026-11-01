# TLA+ 동시성 회귀 검증

TLA+로 중요한 상태와 전이 규칙을 표현하고 TLC로 지정한 작은 구성에서 가능한 전이 순서를 탐색한다. Java 테스트는 실제 코드를 실행하며, TLC는 사람이 작성한 모델을 검사한다. 둘을 함께 사용한다. 탐색 상태 수는 실제 프로그램 실행 횟수가 아니다.

## 검증 대상

| 모델 | 검사하는 계약 | 경계 구성 |
| --- | --- | --- |
| [BlindDateLifecycle](../../tla/BlindDateLifecycle.tla) | 접수된 선택의 순서·중복 실행 방지, timeout/reset 전 기존 작업 완료, 초기화 대기 중 개설 차단, 공정한 실행과 외부 호출 반환을 가정한 종료 | [matrix](../../tla/BlindDateLifecycle.matrix)의 8개 구성: 세션 1~3, 요청 0/1/3/5, worker 1/2/3/5, reset 0/1/2. 전체 수치의 직교 조합은 아님 |

항상 유지돼야 하는 조건을 invariant라 부르고, 결국 종료돼야 한다는 조건을 liveness property라 부른다. 각 속성의 이름과 경계 도달 조건은 모델에, 활성 검사·구성은 matrix에, 대응 결함은 [mutations](../../tla/BlindDateLifecycle.mutations)에 둔다. 변이 검사는 원본을 바꾸지 않고 복사본에 결함을 심어 속성이 실제로 실패하는지 확인한다. `MISSED`나 `UNCOVERED`는 검증 실패다.

모델이 추상화하는 소스 위치와 전체 가정·제외 범위의 원본은 모델 header다. 한 프로세스의 선택·마감·초기화만 다루며 전체 서비스, DB 트랜잭션, 임의 크기의 시스템이 올바르다는 증명은 아니다. Java 코드를 변경해도 모델이 자동 변경되지는 않는다.

## 실행

필수 도구는 JDK 17, Python 3, Bash, tar, curl과 sha256sum 또는 shasum이다. `JAVA_HOME` 또는 실행 가능한 Java 경로를 `JAVA`로 지정할 수 있다.

```bash
bash scripts/tla.sh setup
bash scripts/tla.sh all
```

setup만 네트워크로 도구를 준비한다. 이후 check/mutations/all은 캐시를 읽으며 다운로드하지 않는다.

```bash
bash scripts/tla.sh check
bash scripts/tla.sh mutations
python3 -m unittest discover -s scripts/tests -p 'test_tla_tools.py' -v
./gradlew test --tests '*BlindDateLifecycleConcurrencyTest'
```

개인 Codex 스킬 설치는 필요 없다. [toolchain.env](../../tla/toolchain.env)에 고정된 공식 TLC release와 formal-verify commit을 준비하고 두 아카이브의 SHA256을 확인한다. 캐시는 `.tla-tools/`이며 커밋하지 않는다. 실행 때도 해시를 확인하고 검증된 도구 소스를 새 임시 폴더에 추출한다. `TLC_JAR` 환경 변수로 고정 버전을 우회하지 않는다. `TLA_TOOLS_DIR`로 캐시 디렉터리를 바꿀 수 있다.

`TLC_WORKERS`는 검사 도구의 병렬 탐색 스레드 수다(기본 2). matrix의 `Workers`는 실제 세션 worker 수를 나타내므로 서로 다른 설정이다.

도구 없음·실행 불가·해시 불일치는 `UNAVAILABLE`과 종료 코드 3으로 보고하며 미검증 상태다. 모델/변이 실패는 종료 코드 1, 잘못된 명령은 2다. 오류를 성공으로 처리하지 않는다. 손상된 캐시는 해당 파일을 삭제하고 setup으로 다시 준비한다.

## PR CI와 유지보수

TLA workflow는 모든 PR·merge queue·main push에서 준비, matrix 검사, 변이 검사를 수행한다. 도구 준비 스크립트의 오류 전파·무결성 검사도 확인한다. 일반 Gradle 테스트에는 실제 큐를 latch로 제어하는 `BlindDateLifecycleConcurrencyTest`가 포함된다. worker 1/2/5에서 선택·timeout·초기화 두 건의 순서와 초기화 사이 접수 거부를 관찰한다.

모델 header의 큐·handler·scheduler·service·storage를 바꾸면 실제 동작과 모델의 전이가 여전히 대응하는지 리뷰한다. 정책 결정은 [과팅 도메인 문서](../domains/blinddate.md)를 따른다. header의 소스 범위, matrix 경계, 변이도 함께 갱신하고 공용 all 명령을 재실행한다. 실패한 원본 모델의 속성을 약화하거나 예상 실패로 바꿔 통과시키지 않는다. 실제 코드와 반례가 일치하는지 먼저 확인한다.

도구를 갱신할 때는 revision/version과 SHA256을 함께 검토하고 모든 모델·변이를 다시 실행한다. 외부 helper는 [agent-formal-verify](https://github.com/michael-denyer/agent-formal-verify)의 GPL-3.0-only 도구를 별도로 내려받아 실행하며 저장소에 복사해 배포하지 않는다. TLC는 [공식 release](https://github.com/tlaplus/tlaplus/releases/tag/v1.7.4)를 사용한다.
