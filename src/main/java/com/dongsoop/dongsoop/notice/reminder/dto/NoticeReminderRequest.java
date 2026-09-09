package com.dongsoop.dongsoop.notice.reminder.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record NoticeReminderRequest(
        @NotNull
        LocalDateTime remindAt
) {
}
