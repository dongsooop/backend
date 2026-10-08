-- 채팅 메시지 신고 컬럼
ALTER TABLE report ADD COLUMN IF NOT EXISTS chat_room_id VARCHAR(64);
ALTER TABLE report ADD COLUMN IF NOT EXISTS message_id VARCHAR(64);
ALTER TABLE report ADD COLUMN IF NOT EXISTS message_content VARCHAR(1000);
ALTER TABLE report ADD COLUMN IF NOT EXISTS message_sent_at TIMESTAMP(6);
ALTER TABLE report ADD COLUMN IF NOT EXISTS message_context TEXT;
ALTER TABLE report ADD COLUMN IF NOT EXISTS message_context_after TEXT;
ALTER TABLE report ADD COLUMN IF NOT EXISTS is_auto_reviewed BOOLEAN NOT NULL DEFAULT FALSE;

-- 같은 신고자가 같은 메시지를 두 번 신고하는 경합 방지. 게시판 신고는 message_id가 NULL이라 걸리지 않는다
CREATE UNIQUE INDEX IF NOT EXISTS uk_report_reporter_message ON report (reporter_id, message_id);

-- Hibernate가 enum 컬럼에 만든 허용값 CHECK 제약은 ddl-auto가 갱신하지 않아 새 enum 값 저장을 막는다
ALTER TABLE report DROP CONSTRAINT IF EXISTS report_report_type_check;
ALTER TABLE sanction DROP CONSTRAINT IF EXISTS sanction_sanction_type_check;
