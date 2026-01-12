package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.exception.InvalidGoogleTokenException;
import com.dongsoop.dongsoop.oauth.validator.MemberSocialAccountValidator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class GoogleSocialProvider implements SocialProvider {

    private static final String SERVICE_NAME = "google";
    private static final String ATTRIBUTE_NAME = "sub";

    private final MemberSocialAccountValidator memberSocialAccountValidator;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String googleUserInfoUrl;

    @Value("${spring.security.oauth2.client.provider.google.user-name-attribute}")
    private String googleUserNameAttribute;

    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public String extractProviderId(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        return String.valueOf(attributes.get(ATTRIBUTE_NAME));
    }

    @Override
    public Long login(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    googleUserInfoUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = (Map<String, Object>) response.getBody();

            String providerId = body.get(googleUserNameAttribute).toString();

            MemberSocialAccountDto socialAccount = memberSocialAccountValidator.validate(providerId);
            return socialAccount.member().getId();

        } catch (HttpClientErrorException e) {
            throw new InvalidGoogleTokenException();
        }
    }
}
