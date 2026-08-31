-- PR #344 전용, 배포 시 1회만 수동 실행. Flyway/Liquibase 미사용 프로젝트라 이 저장소의
-- 기존 관례(수동 DDL을 PR 설명에 문서화)를 따라 스크립트 파일로 남겨둔다.
-- 파일명의 PR 번호는 실제 PR 생성 후 맞출 것.
--
-- 배경: 이 PR부터 공지 키워드 알림이 회원 단위(notice_keyword.member_id)에서
-- 기기 단위(notice_keyword.member_device_id)로 바뀐다. 학과 구독은 이미 338에서
-- device_notice_preference 로 기기 단위가 됐는데 키워드만 회원 단위로 남아 있어서,
-- 로그아웃해 member_device.member_id 가 null 이 되는 순간 그 기기는 게스트로 취급되고
-- 키워드 설정이 통째로 무시됐다(EXCLUDE 걸어둔 공지가 다시 발송됨).
--
-- 이 프로젝트는 ddl-auto: update 라 새 컬럼은 자동 생성되지만 옛 컬럼은 삭제되지 않는다.
-- member_id 가 NOT NULL 제약을 단 채 남으면 신규 키워드 INSERT 가 전부 실패하므로,
-- 아래 4단계를 반드시 순서대로 실행한다.
--
-- 실행 시점: 새 코드 배포 전에 1~3단계, 배포 직후 4단계. 앱 트래픽을 받기 전.
--            (4단계를 먼저 하면 구 버전 코드가 member_id 를 못 찾아 죽는다)
-- 되돌리기: 4단계의 DROP COLUMN member_id 는 복구 불가다. 실행 전 notice_keyword 를 백업할 것.
-- DB: PostgreSQL 기준. prod 드라이버가 env(DATABASE_DRIVER_CLASS_NAME)로 주입되어
--     직접 확인하지는 못했다. GenerationType.SEQUENCE 사용과 boolean default false
--     컬럼 정의로 추정했으니 실행 전 확인할 것.
-- 시퀀스명: 엔티티가 @SequenceGenerator(name = "notice_keyword_sequence_generator") 로만
--          선언해 sequenceName 을 지정하지 않았다. Hibernate 는 이때 generator name 을
--          그대로 시퀀스명으로 쓴다. 실행 전 아래로 실제 이름을 확인할 것.
--          SELECT sequencename FROM pg_sequences WHERE sequencename LIKE '%notice_keyword%';
-- 멱등성: 이미 옮겨진 (member_device_id, keyword, type) 조합은 건너뛴다.


-- 1) 새 컬럼 추가. ddl-auto 가 만들어주기 전에 데이터를 옮겨야 하므로 먼저 직접 만든다
ALTER TABLE notice_keyword
    ADD COLUMN IF NOT EXISTS member_device_id BIGINT;


-- 2) 기존 회원 키워드를 그 회원이 보유한 모든 기기로 복제한다.
--    회원이 기기를 여러 대 갖고 있으면 기기 수만큼 행이 생기는 게 정상이다
INSERT INTO notice_keyword (id, member_device_id, keyword, type, is_deleted, created_at, updated_at)
SELECT nextval('notice_keyword_sequence_generator'),
       md.id,
       nk.keyword,
       nk.type,
       nk.is_deleted,
       nk.created_at,
       nk.updated_at
FROM notice_keyword nk
         JOIN member_device md ON md.member_id = nk.member_id
WHERE nk.member_device_id IS NULL
  AND nk.member_id IS NOT NULL
  AND NOT EXISTS (SELECT 1
                  FROM notice_keyword x
                  WHERE x.member_device_id = md.id
                    AND x.keyword = nk.keyword
                    AND x.type = nk.type);


-- 3) 회원 단위였던 원본 행을 제거한다.
--    기기가 하나도 없는 회원의 키워드는 여기서 사라진다 — 옮길 대상 기기가 없으므로 의도된 동작이다
DELETE
FROM notice_keyword
WHERE member_device_id IS NULL;


-- 4) 제약 정리. 새 코드 배포 직후 실행한다
ALTER TABLE notice_keyword
    ALTER COLUMN member_device_id SET NOT NULL;

ALTER TABLE notice_keyword
    DROP COLUMN member_id;

-- 엔티티에 foreignKeyDefinition 을 선언해 두었으므로 ddl-auto: update 가 배포 시
-- 이 FK 를 먼저 만들어 놓았을 수 있다. 그 경우 이름 중복으로 실패하므로 존재 여부를 보고 건너뛴다
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_constraint
                       WHERE conname = 'fk_notice_keyword_member_device') THEN
            ALTER TABLE notice_keyword
                ADD CONSTRAINT fk_notice_keyword_member_device
                    FOREIGN KEY (member_device_id) REFERENCES member_device (id) ON DELETE CASCADE;
        END IF;
    END
$$;


-- 검증: 옮긴 키워드 수가 (회원별 키워드 수 x 그 회원의 기기 수) 합계와 맞는지 확인
--
-- SELECT COUNT(*) FROM notice_keyword;
-- SELECT COUNT(*) FROM notice_keyword WHERE member_device_id IS NULL;  -- 0 이어야 한다
