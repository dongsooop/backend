package com.dongsoop.dongsoop.oauth.provider;

import org.springframework.security.oauth2.core.user.OAuth2User;

public abstract class SocialProvider {

    public abstract String serviceName();

    public abstract String extractProviderId(OAuth2User oAuth2User, String registrationId);
}
