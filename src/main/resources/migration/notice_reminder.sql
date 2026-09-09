-- 공지 리마인더 기능 배포 시 1회 실행.
-- 이 프로젝트는 Flyway/Liquibase를 사용하지 않으므로 기존 migration 관례대로 수동 적용한다.

CREATE TABLE IF NOT EXISTS notice_reminder (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    notice_details_id BIGINT NOT NULL,
    remind_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    claimed_until TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_notice_reminder_member
        FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    CONSTRAINT fk_notice_reminder_notice_details
        FOREIGN KEY (notice_details_id) REFERENCES notice_details(id) ON DELETE CASCADE,
    CONSTRAINT uk_notice_reminder_member_notice
        UNIQUE (member_id, notice_details_id)
);

CREATE INDEX IF NOT EXISTS idx_notice_reminder_status_remind_at
    ON notice_reminder (status, remind_at);

CREATE INDEX IF NOT EXISTS idx_notice_reminder_claimed_until
    ON notice_reminder (claimed_until);
