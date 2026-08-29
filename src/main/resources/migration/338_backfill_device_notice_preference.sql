-- PR #338 전용, 배포 시 1회만 수동 실행. Flyway/Liquibase 미사용 프로젝트라 이 저장소의
-- 기존 관례(수동 DDL을 PR 설명에 문서화)를 따라 스크립트 파일로 남겨둔다.
--
-- 배경: 이 PR부터 공지 알림 발송 대상이 Member.department 자동 조회 방식에서
-- device_notice_preference(디바이스별 명시적 학과 구독) 기준으로 전면 통일된다.
-- 기존 회원은 이 테이블에 아무 행도 없으므로, 그대로 두면 배포 즉시 모든 기존 회원이
-- 학과 공지 알림을 못 받게 된다. 그래서 배포 시 기존 회원의 현재 학과(Member.department)를
-- 그 회원이 보유한 모든 디바이스에 초기 구독으로 심어준다.
--
-- 반드시 이 PR을 배포할 때 딱 한 번만 실행한다. 이후 PR(홈 화면 다학과 집계,
-- 공지 목록 다학과 조회 등)을 배포할 때는 실행하지 않는다 — 그 PR들은 이 테이블을
-- 읽기만 할 뿐 초기 데이터가 필요한 신규 컬럼/개념을 도입하지 않는다.
--
-- 실행 시점: ddl-auto: update 로 device_notice_preference 테이블이 생성된 직후,
-- 앱 트래픽을 받기 전.
-- 멱등성: 이미 존재하는 (member_device_id, department_id) 조합은 건너뛴다 — 재실행해도
-- 안전하지만, 그래도 원칙적으로는 1회만 실행한다.

INSERT INTO device_notice_preference (member_device_id, department_id)
SELECT md.id, m.department_id
FROM member_device md
         JOIN member m ON md.member_id = m.id
WHERE md.member_id IS NOT NULL
  AND m.department_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM device_notice_preference dnp
    WHERE dnp.member_device_id = md.id
      AND dnp.department_id = m.department_id
);

-- 검증: 회원 소유 디바이스 수와 방금 채운 구독 행 수가 대략 일치하는지 확인
-- (한 회원이 디바이스를 여러 개 갖고 있으면 디바이스 수만큼 행이 생기는 게 정상이다)
--
-- SELECT COUNT(*) FROM member_device WHERE member_id IS NOT NULL;
-- SELECT COUNT(*) FROM device_notice_preference;
