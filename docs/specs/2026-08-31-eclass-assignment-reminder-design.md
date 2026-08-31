# 이클래스 과제 마감 리마인드 — 설계 기획서

작성일: 2026-08-31
대상 레포: dongsooop/backend
상태: 구현 완료 (2026-08-31). 구현 과정에서 연동 주체가 "회원"에서 "기기"로 바뀌었고, 홈 과제 탭이 추가되었다 — 아래 본문은 구현된 구조를 반영한다.

---

## 1. 배경과 목표

이클래스(https://eclass.dongyang.ac.kr)는 원격수업·과제 제출이 이루어지는 학교 LMS다. 학생은 과제 마감을 이클래스에 들어가서 직접 확인해야 하고, 마감을 놓치는 일이 잦다.

동숲 앱에서 이클래스 계정을 한 번 연동하면, 동숲이 과제 마감일을 주기적으로 가져와 **마감 3일 전·1일 전·당일 아침에 푸시로 알려주고**, 앱 안에서 다가오는 과제 목록을 볼 수 있게 한다.

**비목표 (이번 범위 밖)**
- 브라우저 확장 프로그램. 조사 결과 필요 없음(3절).
- 퀴즈·VOD 출석 마감. API는 열려 있으나(`mod_quiz_get_quizzes_by_courses`) 1차는 과제만 하고 후속으로 붙인다.
- 과제 제출·내용 열람 등 이클래스 기능을 동숲에서 대신 수행하는 것.
- 리마인드 시각·간격을 사용자별로 설정하는 것. 1차는 고정값.

## 2. 핵심 원칙

### 2.1 학생 비밀번호는 동숲 서버에 오지 않는다

앱이 이클래스에 직접 로그인해 **토큰만** 받고, 동숲에는 토큰만 전달한다. 동숲은 그 토큰으로 이클래스 API를 호출한다. 비밀번호를 보관하는 방식은 유출 시 피해가 이클래스 계정 전체로 번지므로 채택하지 않는다.

### 2.2 연동 주체는 회원이 아니라 기기다 (비회원 지원)

학과 구독(`device_notice_preference`)과 같은 기준으로 **`MemberDevice` 1개당 연동 1건**을 둔다. 요청은 `X-Device-Fid` / `X-Device-Token` 헤더로 기기를 식별하며(`NoticePreferenceDeviceResolver` 재사용), 비회원도 그대로 쓸 수 있다. 알림은 기기 단위로 발송하되, 그 기기가 회원 소유면 알림함에도 남기고 비회원 기기면 푸시만 보낸다 — 공지 알림과 같은 규칙이다.

**탈퇴·삭제 정책(하나로 통일)**: 연동은 회원이 아니라 기기에 매달려 있으므로, 회원 탈퇴(`MemberServiceImpl.deleteMember()` — 회원 soft delete + `MemberDevice.bindMember(null)`)로는 연동이 지워지지 않고 익명 기기의 연동으로 남는다. 그 기기를 계속 쓰는 사람이 같은 사람이기 때문에 이게 맞는 동작이다. 연동 데이터가 지워지는 경로는 둘뿐이다 — 사용자가 `DELETE /eclass/link`로 직접 해제하거나, 기기 행이 삭제되어 FK `ON DELETE CASCADE`가 도는 경우(`WebDeviceCleanupScheduler`의 만료 기기 정리 포함)다.

## 3. 이클래스 조사 결과 (2026-08-31 실계정 확인)

| 항목 | 결과 |
|---|---|
| 플랫폼 | Moodle **3.2.2** (2017-03 빌드), Coursemos v2 테마. 로그인은 SSO 없이 아이디·비밀번호 폼 |
| 토큰 발급 | `GET/POST /login/token.php?username&password&service=moodle_mobile_app` → `{token, privatetoken}` **정상 발급** |
| 토큰 검증 | `core_webservice_get_site_info` → userid, fullname, release, 허용 함수 목록 |
| 과제 목록 | `mod_assign_get_assignments` **허용**. 1회 호출로 수강 전체 과목의 과제(과목명, 과제명, duedate, cutoffdate, allowsubmissionsfromdate, 과제 id, 코스모듈 id) 반환. 테스트 계정 기준 41과목 228건 |
| 제출 상태 | `mod_assign_get_submission_status(assignid)` **허용**. `lastattempt.submission.status` = `submitted` / `new` 등 |
| 수강 과목 | `core_enrol_get_users_courses` 허용. **과목 종료일(enddate)이 전부 비어 있음** → 과목으로는 옛 과목을 거를 수 없다 |
| 다가오는 이벤트 | `core_calendar_get_action_events_by_timesort`, `core_calendar_get_calendar_upcoming_view` **없음** (3.3+에서 추가된 함수) |
| 퀴즈 | `mod_quiz_get_quizzes_by_courses` 허용 (후속 범위) |

설계상 결론: **과제 전체를 받아 마감일로 거른다.** 과목 단위 필터는 불가능하고, 이벤트 API가 없으므로 "제출했으면 자동으로 빠지는" 효과는 제출 상태 API를 따로 호출해서 만든다.

## 4. 사용자 흐름

1. 앱 설정에 "이클래스 연동" 진입 → 이클래스 아이디·비밀번호 입력.
2. 앱이 이클래스 `login/token.php`를 직접 호출해 토큰을 받는다. 실패 시(아이디·비밀번호 오류) 앱에서 바로 안내.
3. 앱이 동숲 `POST /eclass/link`에 토큰을 보낸다. 동숲은 토큰으로 `core_webservice_get_site_info`를 호출해 유효성과 Moodle 사용자 ID·이름을 확인한 뒤 회원에 저장하고, **즉시 첫 수집을 수행**해 연동 직후 앱에 과제 목록이 보이게 한다.
4. 이후 동숲이 주기적으로 과제를 수집하고, 매일 아침 8시에 D-3 / D-1 / D-day 과제를 과제 한 건마다 `[과목명] 과제 n일 전입니다` 형식으로 푸시한다.
5. 앱의 과제 화면은 `GET /eclass/assignments`로 목록을 본다.
6. 연동 해제는 `DELETE /eclass/link` — 연동과 과제 데이터를 함께 삭제한다.
7. 토큰이 무효해지면(유효기간 만료, 비밀번호 변경, 이클래스에서 보안키 삭제 등) 연동을 "만료" 상태로 바꾸고, 먼저 **사일런트 푸시(`ECLASS_RELINK`)** 로 앱에 재발급을 지시한다. 앱은 기기 보안 저장소에 보관한 이클래스 아이디·비밀번호로 `login/token.php`를 다시 호출해 새 토큰을 받고 `POST /eclass/link`로 넘긴다 — 사용자는 아무것도 보지 못한다. 24시간 안에 재연동이 들어오지 않으면(앱이 오래 안 열림, 비밀번호가 바뀌어 재발급 실패 등) 그때 눈에 보이는 재연동 안내 알림을 **한 번만** 보낸다.

## 5. 설계

### 5.1 데이터 모델

**`eclass_link` — 회원당 1행**

| 컬럼 | 타입 | 의미 |
|---|---|---|
| `member_device_id` | FK member_device, unique | 기기 (1:1) |
| `moodle_user_id` | BIGINT | 이클래스 사용자 ID (site_info.userid) |
| `moodle_fullname` | VARCHAR(50) | 연동 확인 화면 표시용 |
| `token_encrypted` | VARCHAR(512) | AES-256-GCM 암호문(IV 포함, Base64) |
| `status` | ENUM ACTIVE / EXPIRED | 만료 시 수집·발송 중단 |
| `linked_at` | TIMESTAMP | 연동 시각 |
| `last_synced_at` | TIMESTAMP nullable | 마지막 수집 성공 시각 |
| `expired_notified_at` | TIMESTAMP nullable | 만료 안내 발송 시각 (중복 발송 방지) |
| `relink_requested_at` | TIMESTAMP nullable | 사일런트 재발급 지시를 보낸 시각. 24시간 지나도 재연동이 없으면 보이는 알림으로 승격 |

`privatetoken`은 저장하지 않는다(자동 로그인 URL 생성용이라 우리 용도에 불필요하고 권한이 더 넓다).

**`eclass_assignment` — 회원별 과제 스냅샷**

| 컬럼 | 타입 | 의미 |
|---|---|---|
| `id` | PK | |
| `eclass_link_id` | FK eclass_link | |
| `assign_id` | BIGINT | Moodle 과제 id. `(eclass_link_id, assign_id)` 유니크 |
| `course_module_id` | BIGINT | 이클래스 과제 페이지 링크용 (`/mod/assign/view.php?id=`) |
| `course_name` | VARCHAR(255) | Moodle 상한과 맞춘다. 넘치면 잘라 저장한다 — 저장 실패로 그 회원 수집이 통째로 멈추면 안 된다 |
| `title` | VARCHAR(255) | 위와 같다 |
| `due_at` | TIMESTAMP | duedate. 0이면 저장하지 않음(마감 없는 과제는 리마인드 대상이 아님) |
| `cutoff_at` | TIMESTAMP nullable | 제출 차단 시각. 앱 표시용 |
| `submitted` | BOOLEAN | 제출 상태 API 결과. `submitted`면 true |
| `submission_checked_at` | TIMESTAMP nullable | |
| `removed_at` | TIMESTAMP nullable | 응답에서 사라진 과제(교수가 삭제). 목록·알림에서 제외 |
| `last_reminded_days` | INT nullable | 마지막으로 보낸 리마인드의 "n일 전" 값(3 → 1 → 0). 같은 단계를 두 번 보내지 않기 위한 멱등 키 |
| `created_at`, `updated_at` | | |

수집 창(오늘 − 1일 ~ 오늘 + 30일) 밖의 과제는 저장하지 않는다. 테스트 계정처럼 과거 과제가 200건 넘게 쌓여 있어도 DB에는 다가오는 것만 남는다.

### 5.2 API

| 메서드 | 경로 | 권한 | 설명 |
|---|---|---|---|
| `POST` | `/eclass/link` | 전체 | body `{ token }`. 토큰 검증 → 저장 → 즉시 수집. 이미 연동돼 있으면 토큰 교체(재연동) |
| `GET` | `/eclass/link` | 전체 | `{ linked, status, moodleFullname, lastSyncedAt }`. 미연동이면 `linked=false` |
| `DELETE` | `/eclass/link` | 전체 | 연동·과제 데이터 삭제 |
| `GET` | `/eclass/assignments` | 전체 | 미제출·마감 전 과제 목록. 마감 오름차순. 항목: 과목, 제목, 마감, D-day, 제출 여부, 이클래스 링크. 미연동이면 400이 아니라 빈 목록 + `linked=false` (기존 비회원 공지 목록과 같은 관례) |
| `POST` | `/eclass/sync` | 전체 | 앱의 당겨서 새로고침용 즉시 수집. 회원당 1분 1회로 제한 |

모든 엔드포인트는 `authentication.path.all`에 `/eclass/**`로 등록해 비회원도 호출할 수 있다(기기 헤더로 식별).

**접근 권한**: 기기 식별자는 인증 수단이 아니므로, 남의 식별자를 헤더에 넣어 그 사람의 데이터에 접근하는 것을 막아야 한다. 규칙은 두 줄이다 — 비회원 기기(회원 미바인딩)는 식별자를 가진 사람이 곧 주인이라 그대로 허용하고, **회원에게 묶인 기기는 로그인한 회원이 그 주인일 때만 허용한다.** 이 판단은 `EclassDeviceAccessor` 한 곳에 두고 `/eclass/**` 전체와 홈 요약이 함께 쓴다 — 과제 조회만 막고 연동 조회·해제·재연동을 열어두면 남의 연동을 지우거나 토큰을 덮어쓸 수 있다. 홈의 회원 경로는 소유가 확인되지 않으면 기기 경로를 버리고 회원 기준 조회로 넘어가고, 비회원 홈 경로도 같은 검사를 거친다. 토큰 검증 실패(`invalidtoken`)는 400 커스텀 예외로 앱에 "토큰이 유효하지 않음"을 명확히 준다.

**홈 화면 과제 탭**: `GET /home`, `GET /home/{departmentType}` 응답에 `eclass_assignment` 요약(`linked`, `upcomingCount`, 가장 임박한 과제의 과목·제목·마감·D-day)이 포함된다. 홈에는 목록을 펼치지 않고 탭(타일)만 그리며, 탭하면 `/eclass/assignments` 화면으로 들어간다. 회원 홈은 요청 기기에 연동이 없으면 그 회원이 가진 다른 기기의 연동까지 본다.

### 5.3 수집 스케줄러 (`EclassSyncScheduler`)

- 주기: **하루 3회** (06:30, 12:30, 18:30 KST, 설정값). 아침 8시 발송 전 06:30 수집이 반드시 돌아 최신 제출 상태로 발송된다.
- **학교 서버로 나가는 요청을 줄이는 것이 이 설계의 제약이다.** 모든 호출이 동숲 서버 IP 하나에서 나가므로, 순간 요청 속도가 높으면 스크래핑으로 보여 차단당할 수 있고 그러면 연동한 전 사용자의 기능이 한 번에 멈춘다. 그래서 (1) 제출 여부는 리마인드가 나가는 기간(`submission-check-days`, 기본 3일)에 든 과제만 확인하고, (2) 스레드 2개·요청 간격 300ms로 속도를 낮추며, (3) 실패 비율이 임계치를 넘으면 남은 연동을 건드리지 않고 그 주기를 접는다. 배포 전에 학교 IT에 알리고 IP 허용을 받아두는 것이 전제다.
- 대상: `eclass_link.status = ACTIVE` 전체. 연동 단위(= 기기 단위)로 스레드 풀(설정값, 기본 2)에 나눠 처리하되, 연동 하나 안의 호출은 순차로 보낸다.
- 회원 1명 처리:
  1. `mod_assign_get_assignments` 1회 → 응답 전체에서 `duedate`가 수집 창 안인 과제만 추림.
  2. 추린 과제 중 **마감이 `submission-check-days` 안에 든 것만** `mod_assign_get_submission_status(assignid)` 호출 → `submitted` 갱신. 이미 `submitted=true`인 과제는 다시 묻지 않는다(제출 취소는 드물고, 놓쳐도 알림이 안 갈 뿐 잘못 가진 않음).
  3. upsert. 이번 응답에 없는데 DB에 있는 창 안 과제는 `removed_at` 기록.
  4. `last_synced_at` 갱신.
- 호출량: 연동 1건당 `1 + (제출 확인 대상 과제 수)`. 제출 확인을 마감 3일 이내로 좁혀 학기 중 평균 1건 남짓이므로, 연동 1,000건이면 1회 수집에 약 2,000 호출이다. 요청 간 딜레이(기본 300ms)와 스레드 2개로 순간 속도를 초당 2회 수준으로 눌러 둔다. 더 줄여야 하면 주기·스레드·딜레이를 yml에서 조정한다.
- 실패 처리:
  - `invalidtoken` → `status=EXPIRED`, 회원의 모든 기기에 사일런트 푸시 `ECLASS_RELINK` 발송, `relink_requested_at` 기록, 이후 수집 대상에서 제외. 앱이 재발급해 `POST /eclass/link`를 호출하면 `status=ACTIVE`로 복귀하고 즉시 수집한다.
  - 만료 승격 점검(수집 스케줄러 끝에 수행): `status=EXPIRED AND relink_requested_at < 지금 − 24h AND expired_notified_at IS NULL`인 회원에게 보이는 알림 "이클래스 연동이 만료되었습니다. 설정에서 다시 연동해 주세요"를 1회 발송하고 `expired_notified_at` 기록.
  - 네트워크·타임아웃·기타 예외 → 해당 회원만 건너뛰고 로그. 다음 주기 재시도. 연동 상태는 바꾸지 않는다.
  - 실패 비율이 임계치(설정, 기본 50%)를 넘으면 **남은 연동을 건드리지 않고 그 주기를 즉시 접는다.** 표본이 작을 때 한두 건으로 접지 않도록 최소 5건을 시도한 뒤부터 비율을 본다.
- 트랜잭션: 외부 호출은 트랜잭션 밖. 회원별 upsert만 짧게.

### 5.4 리마인드 발송 (`EclassReminderScheduler`)

- 시각: **매일 08:00 KST**. 기존 `CalendarScheduler`와 같은 시각이지만 별도 알림으로 보낸다(합치면 학사일정 알림을 끈 사람이 과제 알림도 못 받는다).
- 대상 과제: `submitted=false AND removed_at IS NULL AND due_at::date ∈ {오늘, 내일, 3일 뒤}`.
  마감이 자정(00:00)인 과제가 많다(테스트 계정의 자바프로그래밍 과제 전부). "오늘 00:00 마감"은 이미 지난 것이므로, **날짜 계산은 `due_at`을 기준으로 하되 `due_at <= 지금`인 과제는 제외**한다. 결과적으로 자정 마감 과제는 D-1 아침(=마감 전날 8시)에 마지막 알림을 받는다.
- **과제 1건당 알림 1건**. 묶지 않는다 — 어느 과목 과제가 며칠 남았는지가 제목만 보고 바로 읽혀야 한다.
  - 제목: `[과목명] 과제 n일 전입니다`. 당일은 `[과목명] 과제 오늘 마감입니다`. 예: `[자료구조] 과제 3일 전입니다`, `[자바프로그래밍] 과제 1일 전입니다`.
    과목명이 길면 제목이 잘리므로 과목명은 20자에서 자르고 `…`을 붙인다(`진로지도(컴소 1-C)` 같은 분반 표기는 그대로 둔다).
  - 본문: `과제명 · 마감 9월 25일 (목) 23:55`. 마감이 자정(00:00)이면 `마감 9월 29일 (목) 00:00`으로 그대로 보여준다 — "28일 밤까지"로 바꿔 쓰면 이클래스 화면과 달라 혼란을 준다.
  - `value`(링크): 이클래스 과제 페이지 URL `https://eclass.dongyang.ac.kr/mod/assign/view.php?id={course_module_id}`. 앱이 열면 이클래스 로그인 후 과제로 바로 간다. 앱 내 과제 화면 딥링크가 생기면 그쪽으로 바꾼다.
  - 한 회원이 같은 날 받을 수 있는 알림 상한은 두지 않는다(마감이 겹친 과제는 전부 알려주는 게 목적). 단 알림함 저장은 과제마다 1행씩 쌓이므로 알림 목록이 길어지는 것은 감수한다.
- 알림 타입: **`ECLASS_ASSIGNMENT(true)` 신설**. `NotificationType`에 값을 추가하면 설정 조회(`NotificationSettingServiceImpl`)와 기본값 처리가 자동으로 따라온다. **앱이 모르는 타입을 받았을 때 크래시하지 않는지 앱 팀 확인 필요**(연동 화면과 함께 배포하면 해결).
- 발송 경로: `CalendarNotificationImpl.saveAndSendForMember`와 같은 흐름 — 알림함 저장 후 `MemberDeviceService.getDeviceByMember(조건: 회원 목록, ECLASS_ASSIGNMENT)`로 켜둔 기기만 발송. 비회원 기기도 받는다 — 연동이 기기 단위라 로그인하지 않아도 리마인드가 간다. 다만 알림함(`MemberNotification`)은 회원 기기에만 남기고, 비회원 기기에는 푸시만 보낸다(공지 알림과 같은 규칙).
- 중복 방지: 과제마다 `last_reminded_days`를 두고, 이번에 보낼 단계(3/1/0)가 그 값보다 작을 때만 보낸다. 같은 날 스케줄러가 두 번 돌아도, 마감이 연장돼 다시 D-3이 되어도 이미 보낸 단계는 반복하지 않는다. 마감 연장으로 단계가 뒤로 밀린 경우(D-1 보냈는데 다시 D-5가 됨)는 `last_reminded_days`를 null로 되돌려 새 마감 기준으로 다시 알린다.

### 5.5 토큰 보호

- **토큰 수명**: 영구라고 가정하지 않는다. Moodle 토큰은 (1) 관리자가 정한 유효기간이 지나면, (2) 사용자가 비밀번호를 바꾸면, (3) 사용자가 이클래스 "보안키 관리"에서 삭제하면 무효가 된다. 어느 경우든 **만료를 감지한 뒤 재발급하는 흐름(4절 7번) 하나로 처리한다.** 실제 유효기간은 로그인 후 `/user/managetoken.php`(환경설정 → 보안키)의 "유효기간" 열에서만 확인할 수 있어 서버가 알 방법이 없다 — 값을 알게 되면 그때 만료 전 선제 재발급을 넣는다.
  `login/token.php`는 유효한 토큰이 이미 있으면 새로 만들지 않고 같은 토큰을 돌려주므로, 앱이 재연동을 다시 요청해도 서버 쪽 처리는 동일하다.
  **자동 재발급(확정)**: 토큰 만료 시각은 발급 때 정해지고 서버가 늘릴 방법이 없으므로, "연장"은 곧 `login/token.php` 재호출이고 그 호출에는 비밀번호가 필요하다. 서버는 비밀번호를 갖지 않는다는 원칙을 지키기 위해 **앱이 기기 보안 저장소(iOS Keychain / Android Keystore)에 이클래스 아이디·비밀번호를 보관**하고, 서버가 보내는 사일런트 푸시 `ECLASS_RELINK`를 받으면 백그라운드에서 토큰을 재발급해 `POST /eclass/link`로 넘긴다. 사용자에게는 아무것도 보이지 않는다. 비밀번호가 바뀌어 재발급이 실패하면 앱은 보관한 비밀번호를 지우고, 서버는 24시간 뒤 보이는 알림으로 재입력을 안내한다.
  **선제 재발급은 넣지 않았다.** 만료 시각을 알 수 있는 API가 없어(`/user/managetoken.php`는 로그인 세션이 필요한 화면이다) 채울 값이 없고, 값이 없는 채로 코드만 두면 한 번도 돌지 않는 죽은 경로가 된다. 유효기간을 실측해 확인한 뒤 그때 넣는다. 그때까지는 만료를 감지한 뒤 재발급하는 아래 경로만으로 동작한다.
  **사일런트 푸시 전달 보장**: iOS는 백그라운드 푸시를 배터리·사용 패턴에 따라 지연하거나 버릴 수 있다. 그래서 앱은 푸시와 무관하게 **앱을 열 때마다 `GET /eclass/link`를 조회해 `status=EXPIRED`이면 스스로 재발급**한다. 사일런트 푸시는 빠른 경로일 뿐 유일한 경로가 아니다.
- 저장: AES-256-GCM, 키는 환경변수 `ECLASS_TOKEN_KEY`(32바이트 Base64). 현재 레포에 암호화 유틸이 없으므로 `common/crypto/AesGcmEncryptor` 신설. 키 로테이션은 1차 범위 밖(필요 시 재연동 안내로 대체).
- 로그에 토큰·응답 전문을 남기지 않는다. 이클래스 호출 실패 로그는 함수명·errorcode만.
- 응답 DTO에 토큰을 절대 포함하지 않는다.
- 사용자는 이클래스 "보안키 관리"에서 언제든 토큰을 폐기할 수 있고, 그 경우 다음 수집에서 EXPIRED로 전환된다.
- `DELETE /eclass/link` 시 `eclass_link`와 `eclass_assignment`를 함께 삭제한다. 회원 탈퇴로는 지우지 않는다(위 탈퇴·삭제 정책 참고).

### 5.6 이클래스 API 클라이언트 (`EclassClient`)

- 기존 소셜 로그인 프로바이더처럼 `RestTemplate` 기반. 베이스 URL·타임아웃·user-agent는 yml.
- 함수: `getSiteInfo(token)`, `getAssignments(token)`, `getSubmissionStatus(token, assignId)`.
- Moodle 웹서비스는 오류도 HTTP 200으로 `{exception, errorcode}`를 주므로 응답 본문의 `exception` 필드로 판별해 `EclassInvalidTokenException` / `EclassApiException`으로 매핑한다.
- 응답에서 쓰는 필드만 DTO에 담는다(`@JsonIgnoreProperties(ignoreUnknown = true)`). Moodle 3.2 응답 스키마에 맞춰 작성하되, 3절의 실측 응답을 테스트 픽스처로 저장한다.

### 5.7 설정값 (`eclass.*`)

| 키 | 기본값 |
|---|---|
| `base-url` | `https://eclass.dongyang.ac.kr` |
| `connect-timeout-ms` / `read-timeout-ms` | 5000 / 15000 |
| `sync.cron` | `0 30 6,12,18 * * *` |
| `sync.window-past-days` / `sync.window-future-days` | 1 / 30 |
| `sync.submission-check-days` | 3 |
| `sync.thread-count` | 2 |
| `sync.request-delay-ms` | 300 |
| `sync.abort-failure-ratio` | 0.5 |
| `sync.manual-cooldown-seconds` | 60 |
| `reminder.cron` | `0 0 8 * * *` |
| `reminder.days-before` | `[0, 1, 3]` |
| `reminder.course-name-max-length` | 20 |
| `token-key` | prod는 환경변수 `ECLASS_TOKEN_KEY`, 로컬·테스트는 프로필 파일의 고정값. 공유 `application.yml`에는 두지 않는다 |

### 5.8 엣지 케이스

| 상황 | 동작 |
|---|---|
| 마감 없는 과제(duedate=0) | 저장·알림 대상 아님 |
| 자정 마감 과제 | 마감 전날 08:00이 마지막 알림. "오늘 마감"으로 오인해 지난 과제를 알리지 않음 |
| 제출 후 교수가 재제출 요구(status가 `reopened`) | `submitted=false`로 되돌려 다시 알림. 단 이미 true인 과제는 재조회하지 않으므로 다음 재연동/수동 동기화 전까지는 놓칠 수 있음 — 1차에서 감수, 로그로 빈도 확인 후 재조회 정책 조정 |
| 교수가 마감 연장 | 다음 수집에서 `due_at` 갱신 + 리마인드 단계 초기화 → 새 마감 기준으로 D-3부터 다시 알림. 별도 변경 알림은 보내지 않는다 |
| 교수가 마감을 앞당김 | `due_at` 갱신 + **"[과목명] 과제 마감이 앞당겨졌어요" 알림 1회**. 앞당겨진 마감이 이미 지났으면 리마인드 대상에서 빠지므로, 이 알림이 없으면 사용자가 끝까지 모른다 |
| 연동이 끊긴(EXPIRED) 상태의 화면 | 목록·홈 요약이 `linked=true, status=EXPIRED, 빈 목록`을 준다. "과제 없음"으로 보여주면 마감이 없다고 잘못 안심시키므로 앱은 재연동을 안내해야 한다 |
| 교수가 과제 삭제 | `removed_at` 기록, 목록·알림 제외 |
| 같은 이클래스 계정을 여러 기기에서 연동 | 허용한다 — 기기 단위 연동이므로 폰과 태블릿에서 각각 알림을 받는 것이 자연스럽다 |
| 연동 직후 첫 수집이 실패 | 연동은 성공으로 저장하고 `last_synced_at=null`. 앱은 "동기화 대기 중" 표시. 다음 주기에 재시도 |
| 토큰은 유효하나 함수 권한이 바뀜(학교 설정 변경) | `EclassApiException`으로 분류, 연동 상태는 유지, 경고 로그. 전 회원 동일 실패면 임계치로 주기 중단 |
| 앱이 옛 버전이라 새 알림 타입을 모름 | 앱 팀 확인 항목. 확인 전까지 발송 기능 플래그 `reminder.enabled=false`로 배포 가능 |
| 방학 | 창 안 과제 0건 → 호출 1회/회원으로 최소 부하 |
| 마감이 같은 날 겹친 과제 여러 개 | 과제마다 알림이 따로 간다(의도). 알림함에도 과제 수만큼 쌓인다 |
| 연동한 날 이미 D-2인 과제 | 다음 단계인 D-1에 처음 알린다. D-3은 지났으므로 보내지 않는다 |

### 5.9 변경 파일 (예상)

- `eclass/` 신규 패키지: `controller/EclassController`, `service/EclassLinkService`, `service/EclassSyncService`, `client/EclassClient` + 응답 DTO, `entity/EclassLink`, `entity/EclassAssignment`, `repository/*`, `scheduler/EclassSyncScheduler`, `scheduler/EclassReminderScheduler`, `notification/EclassNotification`, `exception/*`, `config/EclassProperties`
- `common/crypto/AesGcmEncryptor` 신규
- `notification/constant/NotificationType` — `ECLASS_ASSIGNMENT(true)` 추가
- `notification/constant/FcmSilentType` — `ECLASS_RELINK` 추가 (기존 `FORCE_LOGOUT`과 같은 data-only 메시지, `FCMServiceImpl.sendSilentMessage` 재사용)
- `application.yml`, `application-prod.yml` — `eclass.*`, 환경변수 키
- 스키마는 `ddl-auto: update`로 생성. 백필 없음 → 마이그레이션 SQL 불필요

### 5.10 테스트 계획

기존 방식(슬라이스 `@SpringBootTest` + `@MockitoBean`, 통합은 Testcontainers)을 따른다.

- `EclassClient`: 실측 응답 픽스처로 역직렬화, `exception` 응답 → 예외 매핑(invalidtoken vs 기타)
- 수집: 창 필터(과거·미래·duedate=0), upsert, removed 처리, submitted 갱신, 이미 submitted면 재조회 안 함, invalidtoken → EXPIRED + 만료 알림 1회, 두 번째 주기엔 알림 없음
- 리마인드: D-0/1/3 선택, `due_at <= now` 제외, 자정 마감 케이스, 과제 1건당 알림 1건, 제목 형식(`[과목명] 과제 n일 전입니다` / `오늘 마감입니다`, 과목명 20자 절단), 링크가 코스모듈 id로 생성됨, 같은 단계 재발송 없음(`last_reminded_days`), 마감 연장 시 단계 리셋, 알림 끈 기기 제외
- 토큰 만료·재발급: invalidtoken → EXPIRED + 사일런트 푸시 1회 + `relink_requested_at` 기록, 24시간 내 재연동 시 보이는 알림 없음, 24시간 경과 시 보이는 알림 1회(두 번째 주기엔 없음), 재연동 시 ACTIVE 복귀 + 즉시 수집, 제출 조회 중 만료돼도 그때까지 모은 과제는 저장
- 접근 권한: 남의 기기 식별자로 과제 조회·홈 요약·연동 조회·해제·재연동이 모두 막히고, 비회원 기기는 로그인 없이 자기 것을 다룰 수 있음
- API: 미연동 시 빈 목록, 재연동 시 토큰 교체, 해제 시 과제 삭제, 수동 동기화 쿨다운, 같은 moodle_user_id 중복 연동 거절
- 암호화: 왕복 일치, 키 없으면 기동 실패(설정 누락을 배포 시점에 잡기)
- 홈 요약: 미연동 기기는 `linked=false`, 연동됐지만 과제가 없으면 `linked=true`·`upcomingCount=0`

## 6. 진행 단계

- **PR 1 — 연동·수집·조회**: 데이터 모델, 암호화, 클라이언트, 연동 API 3개, 수집 스케줄러, 과제 목록 API. 알림 없음. 이 PR만으로 앱 연동 화면과 과제 목록을 붙여 볼 수 있다.
- **PR 2 — 리마인드**: 알림 타입 추가, 발송 스케줄러, 만료 안내 알림, 수동 동기화 API.
- **후속 후보**: 퀴즈 마감(`mod_quiz_get_quizzes_by_courses`의 `timeclose`), 마감 연장 알림, 리마인드 시각 사용자 설정, 당일 저녁 미제출 재알림.

## 7. 앱 측에 필요한 것

1. 이클래스 연동 화면: 아이디·비밀번호 입력 → `login/token.php` 직접 호출 → 토큰을 `POST /eclass/link`로 전달. 비밀번호는 **동숲 서버로 전송하지 않는다.** 토큰 발급 요청을 보낸 직후 메모리에서 지우고, 자동 재발급을 쓸 경우에만 아래 4번의 기기 보안 저장소에 보관한다(그 외 어디에도 남기지 않는다).
2. 과제 목록 화면(`GET /eclass/assignments`)과 당겨서 새로고침(`POST /eclass/sync`).
3. 알림 타입 `ECLASS_ASSIGNMENT` 처리와 설정 화면 항목 추가.
4. 이클래스 아이디·비밀번호를 기기 보안 저장소(iOS Keychain / Android Keystore)에 보관. 앱 서버나 로그로 내보내지 않는다.
5. 사일런트 푸시 `ECLASS_RELINK` 수신 시, 그리고 앱을 열 때 `GET /eclass/link`가 `EXPIRED`이면, 보관한 계정으로 `login/token.php`를 호출해 새 토큰을 받아 `POST /eclass/link`로 전달(백그라운드, UI 없음). 재발급이 아이디·비밀번호 오류로 실패하면 보관한 비밀번호를 삭제.
6. 보이는 만료 알림(24시간 뒤 승격분)을 받았을 때 연동 화면으로 유도. 연동 해제 시 보관한 계정 정보도 삭제.

## 8. 결정이 필요한 항목

1. 리마인드 시점 `D-3, D-1, D-0 08:00` 고정으로 시작해도 되는지.
2. 자정 마감 과제를 "마감 전날 08:00"이 마지막 알림으로 두는 것이 맞는지, 아니면 전날 저녁(예: 20:00) 한 번 더 보낼지.
3. 앱에서 새 알림 타입을 안전하게 받는지(구버전 앱 크래시 여부).
4. 이클래스 토큰 유효기간 실측값(`/user/managetoken.php`의 유효기간 열). 만료가 없으면 선제 재발급 경로는 사실상 쓰이지 않고 비밀번호 변경·보안키 삭제 때만 재발급이 일어난다.

확정된 것: 토큰 자동 재발급은 앱이 기기 보안 저장소에 계정을 보관하고 서버의 사일런트 푸시 또는 앱 실행 시 상태 확인으로 수행한다(2026-08-31 결정).
