package com.dongsoop.dongsoop.notification.dto;

import com.dongsoop.dongsoop.notification.constant.NotificationType;

public record EventNotification(

        String title,
        String body,
        NotificationType type
) {
}
