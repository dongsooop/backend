package com.dongsoop.dongsoop.notification.setting.dto;

import com.dongsoop.dongsoop.notification.constant.NotificationType;

public record NotificationActiveStatus(
        NotificationType notificationType,
        boolean enabled
) {
}
