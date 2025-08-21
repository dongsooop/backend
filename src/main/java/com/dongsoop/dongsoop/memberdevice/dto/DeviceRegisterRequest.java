package com.dongsoop.dongsoop.memberdevice.dto;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceRegisterRequest(

        @NotBlank
        String deviceToken,

        @NotNull
        MemberDeviceType type
) {
}
