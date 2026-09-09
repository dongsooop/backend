package com.dongsoop.dongsoop.notice.reminder.dto;

import java.time.LocalDateTime;

public record NoticeReminderResponse(
        Long noticeId,
        LocalDateTime remindAt
) {
}
