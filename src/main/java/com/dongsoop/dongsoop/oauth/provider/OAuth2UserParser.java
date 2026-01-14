package com.dongsoop.dongsoop.oauth.provider;

public interface OAuth2UserParser {

    SocialProvider extractProvider(String registrationId);
}
