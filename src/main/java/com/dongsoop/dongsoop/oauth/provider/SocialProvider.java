package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.oauth.dto.SocialAccountLinkRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface SocialProvider {

    String serviceName();

    String extractProviderId(OAuth2User oAuth2User, String registrationId);

    Long login(String accessToken);

    void linkSocialAccount(Long memberId, SocialAccountLinkRequest request);
}
