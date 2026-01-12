package com.dongsoop.dongsoop.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(

        @NotBlank
        String token
) {
}
