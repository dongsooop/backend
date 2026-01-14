package com.dongsoop.dongsoop.oauth.dto;

import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import java.time.LocalDateTime;

public record MemberSocialAccountOverview(

        OAuthProviderType providerType,
        LocalDateTime createdAt
) {
}
