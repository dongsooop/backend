package com.dongsoop.dongsoop.notification.dto;

import com.dongsoop.dongsoop.notification.constant.NotificationType;

public record NotificationSend(

        Long id,
        String title,
        String body,
        NotificationType type,
        String value
) {
}
