package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.oauth.dto.SocialAccountLinkRequest;
import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface SocialProvider {

    String serviceName();

    String extractProviderId(OAuth2User oAuth2User, String registrationId);

    Authentication login(String accessToken);

    LocalDateTime linkSocialAccount(Long memberId, SocialAccountLinkRequest request);

    void revoke(String refreshToken);
}
