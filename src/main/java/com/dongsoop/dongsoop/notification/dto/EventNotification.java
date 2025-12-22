package com.dongsoop.dongsoop.notification.dto;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventNotification(

        @NotBlank
        String title,

        @NotBlank
        String body,

        @NotNull
        NotificationType type
) {
}
