# 공지 키워드 알림 기기 단위 전환 — 배포 안내

`344_migrate_notice_keyword_to_device.sql` 과 짝이 되는 문서다.
이 저장소는 Flyway/Liquibase 를 쓰지 않고 수동 DDL 을 문서로 남기는 관례를 따른다.

---

## 무엇이 바뀌나

공지 키워드 알림이 **회원 단위에서 기기 단위**로 바뀐다.

| | 이전 | 이후 |
|---|---|---|
| 키워드 소유자 | `notice_keyword.member_id` | `notice_keyword.member_device_id` |
| 설정 주체 | 로그인한 회원만 | 기기 (회원·비회원 모두) |
| API 인증 | `getMemberIdByAuthentication()` | `X-Device-Fid` / `X-Device-Token` 헤더 |

학과 구독(`device_notice_preference`)은 PR #338 에서 이미 기기 단위가 됐는데 키워드만 회원 단위로 남아 있었다.
그래서 **로그아웃하면 `member_device.member_id` 가 `null` 이 되어 그 기기가 게스트로 취급되고, 키워드 설정이 통째로 무시됐다.**
EXCLUDE 로 걸어둔 공지가 로그아웃 후 다시 발송되던 문제가 이것이다.

부수적으로, 발송 단계에서 학과 구독 조건이 풀려 **구독하지 않은 기기에도 공지가 가던 문제**도 함께 닫힌다.
회원의 전체 기기를 다시 조회하던 `sendAll` 대신, 걸러둔 기기 목록을 그대로 넘기는 `sendAllToDevices` 를 쓴다.

---

## 배포 순서

`ddl-auto: update` 는 컬럼을 **추가만 하고 삭제하지 않는다.**
`member_id` 가 `NOT NULL` 제약을 단 채 남으면 신규 키워드 INSERT 가 전부 실패하므로 순서를 반드시 지킨다.

```
1. (배포 전)   SQL 1~3 단계 실행
2.             새 코드 배포
3. (배포 직후) SQL 4 단계 실행
```

**4 단계를 먼저 실행하면 안 된다.** 구 버전 코드가 `member_id` 컬럼을 찾지 못해 기동에 실패한다.

1~3 단계와 배포 사이에는 앱 트래픽이 없어야 한다.
2 단계에서 기존 키워드를 복제한 뒤 3 단계가 원본 행을 지우는데, 그 사이에 구 코드가 새 키워드를 만들면 함께 지워진다.

---

## 실행 전 확인할 것

SQL 은 아래 두 가지를 **가정**하고 작성했다. 실행 전에 실제 값을 확인한다.

### 1. DB 종류

PostgreSQL 기준으로 작성했다. prod 드라이버가 `${DATABASE_DRIVER_CLASS_NAME}` 로 주입되어 코드에서 확인할 수 없었다.
`GenerationType.SEQUENCE` 사용과 `boolean default false` 컬럼 정의를 근거로 추정했다.

### 2. 시퀀스 이름

엔티티가 `@SequenceGenerator(name = "notice_keyword_sequence_generator")` 로만 선언하고 `sequenceName` 을 지정하지 않았다.
Hibernate 는 이때 generator name 을 그대로 시퀀스명으로 쓴다.

```sql
SELECT sequencename FROM pg_sequences WHERE sequencename LIKE '%notice_keyword%';
```

### 3. 백업

4 단계의 `DROP COLUMN member_id` 는 되돌릴 수 없다. 실행 전에 `notice_keyword` 를 백업한다.

```sql
CREATE TABLE notice_keyword_backup_344 AS SELECT * FROM notice_keyword;
```

---

## 마이그레이션 단계 요약

| 단계 | 하는 일 | 시점 |
|---|---|---|
| 1 | `member_device_id` 컬럼 추가 (nullable) | 배포 전 |
| 2 | 기존 회원 키워드를 그 회원의 **모든 기기로 복제** | 배포 전 |
| 3 | 회원 단위였던 원본 행 삭제 | 배포 전 |
| 4 | `NOT NULL` 설정, `member_id` 삭제, FK 추가 | 배포 직후 |

2 단계는 `member_device.member_id` 로 조인해 **회원이 설정한 키워드를 그 회원의 모든 기기에 복제**한다.
회원이 기기를 여러 대 갖고 있으면 기기 수만큼 행이 생기는 것이 정상이다.

복제되지 않는 경우가 두 가지 있다.

- **기기가 하나도 없는 회원** — 옮길 대상이 없어 3 단계에서 키워드가 사라진다.
- **로그아웃 상태인 기기** — `member_device.member_id` 가 `null` 이라 조인에 걸리지 않는다.
  그 기기가 어느 회원의 것이었는지 알 방법이 없어 불가피하다. 이 기기들은 키워드 없이(= 전체 수신) 시작한다.

4 단계의 FK 추가는 조건부로 감쌌다. 엔티티에 `foreignKeyDefinition` 을 선언해 두어서
`ddl-auto: update` 가 배포 시 같은 이름의 FK 를 먼저 만들어 놓았을 수 있기 때문이다.

### 검증

```sql
-- 0 이어야 한다
SELECT COUNT(*) FROM notice_keyword WHERE member_device_id IS NULL;

-- 복제 결과 확인 (회원별 키워드 수 x 그 회원의 기기 수 합계와 맞는지)
SELECT COUNT(*) FROM notice_keyword;
```

---

## API 두 벌

키워드가 기기 단위로 바뀌면서 요청에서 대상을 정하는 방식이 달라졌다.
기기 헤더를 보내지 않는 구버전 앱을 위해 기존 경로를 그대로 남겨두었다.

| 경로 | 대상 | 용도 |
|---|---|---|
| `/notice/keywords` | 인증된 회원의 **기기 전체** | 구버전 앱. 손대지 않아도 계속 동작한다 |
| `/v2/notice/keywords` | 헤더로 지목한 **기기 하나** | 신버전 앱. 비회원도 쓸 수 있다 |

구버전 경로는 회원의 기기 전체를 한 묶음으로 다뤄 예전처럼 "내 키워드"로 동작한다.
특히 `DELETE` 는 받은 id 의 `(keyword, type)` 을 회원의 **모든 기기에서** 지운다.
그 id 는 특정 기기의 행이라 그것만 지우면 나머지 기기에 남아 알림이 계속 오기 때문이다.

구버전 앱이 충분히 줄면 `NoticeKeywordController` 와
`NoticeKeywordServiceImpl` 의 `...ByMember` 세 메서드를 지우면 된다.

### v2 헤더

```
X-Device-Fid: <Firebase Installation ID>     # 우선 사용
X-Device-Token: <FCM 토큰>                    # fid 가 아직 없는 기기의 폴백
```

`/subscribe-department` 가 쓰는 방식과 같다.
`NoticePreferenceDeviceResolver` 가 fid 를 먼저 보고, 없으면 deviceToken 으로 기기를 찾는다.
둘 다 없거나 등록되지 않은 기기면 `UnregisteredDeviceException` 이 발생한다.

`application.yml` 의 `authentication.path.all` 에 `/v2/notice/**` 를 추가해 두었다.
이게 없으면 비회원이 v2 를 쓸 수 없다.

---

## 앱에서 해야 할 일

**서버만 배포해도 기존 앱은 깨지지 않는다.** 구버전 경로를 남겨두었기 때문이다.

비회원 키워드 기능을 열려면 앱에서 두 가지가 필요하다.

1. 키워드 API 를 `/v2/notice/keywords` 로 바꾸고 위 헤더를 붙인다.
   현재 `notice_keyword_data_source_impl.dart` 가 `_authDio` 를 쓰고 헤더가 없다.
   `subscribe_department_data_source_impl.dart` 가 이미 맞는 형태다.
2. 알림 화면의 키워드 탭이 `if (user != null)` 안에 있어 비회원에게 보이지 않는다. 그 가드를 푼다.

둘 중 하나만 하면 비회원은 여전히 기능에 도달하지 못한다.

---

## 동작 범위

| | 학과 구독 | 키워드 | 알림함 | 배지 |
|---|---|---|---|---|
| 회원 | 기기 단위 | **기기 단위** | 회원 단위 | 회원 단위 |
| 비회원 | 기기 단위 | **기기 단위** | 없음 | 없음 |
| 로그아웃한 기기 | 유지 | **유지** | 없음 | 없음 |

알림함과 배지는 회원 전용으로 **의도적으로 남겨두었다.**
푸시는 비회원도 받고 알림함은 계정 단위로 두는 것이 일반적인 구성이고,
알림함을 기기 단위로 뒤집으면 회원의 읽음 상태가 기기별로 갈리는 문제가 생긴다.

---

## 후속 작업 (이번 PR 범위 밖)

이번 변경과 무관하거나 전역 영향이 있어 손대지 않은 항목이다.

### 1. Hibernate 배치 INSERT 미설정

`application.yml` 에 `hibernate.jdbc.batch_size` 설정이 없다.
알림함 저장(`notificationRepository.saveAll`)이 대상 회원 수만큼 개별 INSERT 를 날린다.

켜면 효과가 크지만 **전역 설정이라 모든 기능에 영향**을 준다.
`order_inserts` 도 함께 켜야 실제 배치가 이뤄지므로 별도로 검토한다.

### 2. `FCMServiceImpl.listener()` 의 도달하지 않는 예외

```java
if (response.getFailureCount() > 0) {
    handleFailure(response, tokens);
    throw new NotificationSendException();   // 아무도 잡지 않는다
}
```

`future.addListener(Runnable, Executor)` 로 등록된 콜백에서 던진 예외는 호출자에게 전파되지 않는다.
`handleFailure` 는 실행되므로 실패한 토큰 정리는 되지만, 이 `throw` 는 하는 일이 없다.

### 3. 배지 계산 쿼리 횟수

공지 단위 루프라 `findUnreadCountByMemberIds` 가 공지 건수만큼 호출된다.
1회로 줄이려면 "전체 저장 → 배지 한 번 계산 → 공지별 발송" 으로 단계를 쪼개야 하는데,
얻는 것은 가벼운 count 쿼리 몇 번이고 구조는 복잡해져 현행을 유지했다.
최종 배지 값은 어느 쪽이든 같다.

### 4. 비회원 알림함

필요해지면 별도 작업으로 다룬다.
`MemberNotificationId` 복합키가 `(details, member)` 이고 둘 다 `NOT NULL` 이라
비회원 행을 만들려면 알림 시스템 전반(생성처 6곳, 조회·읽음·삭제 API)을 함께 바꿔야 한다.
