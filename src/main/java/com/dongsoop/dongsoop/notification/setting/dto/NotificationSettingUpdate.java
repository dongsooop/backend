package com.dongsoop.dongsoop.notification.setting.dto;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record NotificationSettingUpdate(

        @NotBlank
        String deviceToken,

        @NotEmpty
        List<NotificationType> notificationTypes,

        @NotNull
        Boolean targetState
) {
}
