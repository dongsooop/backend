package com.dongsoop.dongsoop.oauth.provider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class OAuth2UserParserImpl implements OAuth2UserParser {

    private final Map<String, SocialProvider> oAuth2UserServiceMap;

    public OAuth2UserParserImpl(List<SocialProvider> oAuth2UserServices) {
        this.oAuth2UserServiceMap = oAuth2UserServices.stream()
                .collect(Collectors.toMap(SocialProvider::serviceName, service -> service));
    }

    @Override
    public String extractProviderId(OAuth2User oAuth2User, String registrationId) {
        SocialProvider provider = this.oAuth2UserServiceMap.getOrDefault(registrationId,
                null);
        if (provider != null) {
            return provider.extractProviderId(oAuth2User, registrationId);
        }

        // 다른 공급자 추가 가능
        throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
    }
}
