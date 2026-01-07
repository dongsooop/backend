package com.dongsoop.dongsoop.notification.setting.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationSettingFindRequest(

        @NotBlank
        String deviceToken
) {
}
