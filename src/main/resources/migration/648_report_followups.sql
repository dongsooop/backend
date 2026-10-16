-- 메시지 신고 시점의 방 종류(ONE_TO_ONE·GROUP·CONTACT·BLINDDATE). 게시판·회원 신고와 이 컬럼 이전 신고는 NULL
ALTER TABLE report ADD COLUMN IF NOT EXISTS chat_room_type VARCHAR(20);

-- 같은 메시지의 다른 신고를 처리하면서 함께 닫힌 신고가 가리키는, 실제로 처리된 신고 ID
ALTER TABLE report ADD COLUMN IF NOT EXISTS resolved_by_report_id BIGINT;
