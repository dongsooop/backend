package com.dongsoop.dongsoop.notification.setting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecruitmentSettingUpdateRequest(

        @NotBlank
        String deviceToken,

        @NotNull
        Boolean targetState
) {
}
