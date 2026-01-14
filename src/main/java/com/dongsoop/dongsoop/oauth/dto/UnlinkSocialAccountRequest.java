package com.dongsoop.dongsoop.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record UnlinkSocialAccountRequest(

        @NotBlank
        String token
) {
}
