package com.dongsoop.dongsoop.notification.setting.dto;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationSettingRequest(

        @NotBlank
        String deviceToken,

        @NotNull
        NotificationType notificationType
) {
}
