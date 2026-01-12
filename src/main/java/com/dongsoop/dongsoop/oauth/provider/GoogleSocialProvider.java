package com.dongsoop.dongsoop.oauth.provider;

import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class GoogleSocialProvider extends SocialProvider {

    private static final String SERVICE_NAME = "google";
    private static final String ATTRIBUTE_NAME = "sub";

    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public String extractProviderId(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        return String.valueOf(attributes.get(ATTRIBUTE_NAME));
    }
}
