-- 과팅은 한 세션에서 같은 상대를 한 번만 신고할 수 있다
CREATE UNIQUE INDEX IF NOT EXISTS uk_report_blinddate_reporter_target
    ON report (reporter_id, chat_room_id, target_member_id)
    WHERE report_type = 'BLINDDATE_MESSAGE';
