package com.dongsoop.dongsoop.notice.reminder.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record NoticeReminderRequest(
        @NotNull
        @Future
        LocalDateTime remindAt
) {
}
