package com.dongsoop.dongsoop.oauth.provider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OAuth2UserParserImpl implements OAuth2UserParser {

    private final Map<String, SocialProvider> oAuth2UserServiceMap;

    public OAuth2UserParserImpl(List<SocialProvider> oAuth2UserServices) {
        this.oAuth2UserServiceMap = oAuth2UserServices.stream()
                .collect(Collectors.toMap(SocialProvider::serviceName, service -> service));
    }

    @Override
    public SocialProvider extractProvider(String registrationId) {
        return this.oAuth2UserServiceMap.get(registrationId);
    }
}
