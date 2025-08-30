package com.dongsoop.dongsoop.notification.dto;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import java.time.LocalDateTime;

public record NotificationList(

        Long id,

        String title,

        String body,

        NotificationType type,

        String value,

        boolean isRead,

        LocalDateTime createdAt
) {
}
