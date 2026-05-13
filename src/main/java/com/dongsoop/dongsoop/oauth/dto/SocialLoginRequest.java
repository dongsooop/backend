package com.dongsoop.dongsoop.oauth.dto;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(

        @NotBlank
        String token,

        String deviceToken,

        MemberDeviceType deviceType
) {
}
