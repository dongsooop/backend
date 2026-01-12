package com.dongsoop.dongsoop.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialAccountLinkRequest(

        @NotBlank
        String providerToken
) {
}
