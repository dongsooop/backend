package com.dongsoop.dongsoop.memberdevice.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceLoginRequest(

        @NotBlank
        String deviceToken
) {
}
