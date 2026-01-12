package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.exception.InvalidKakaoTokenException;
import com.dongsoop.dongsoop.oauth.validator.MemberSocialAccountValidator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class KakaoSocialProvider implements SocialProvider {

    private static final String SERVICE_NAME = "kakao";
    private static final String ATTRIBUTE_NAME = "id";

    private final RestTemplate restTemplate = new RestTemplate();
    private final MemberSocialAccountValidator memberSocialAccountValidator;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String kakaoUserInfoUrl;

    @Value("${oauth.kakao.user-name-attribute}")
    private String kakaoUserNameAttribute;

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
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    kakaoUserInfoUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // 카카오의 응답값 파싱
            Map<String, Object> body = response.getBody();
            String providerId = body.getOrDefault(kakaoUserNameAttribute, null).toString();

            // 회원 검증
            MemberSocialAccountDto socialAccount = memberSocialAccountValidator.validate(providerId);

            return socialAccount.member().getId();

        } catch (HttpClientErrorException e) {
            throw new InvalidKakaoTokenException();
        }
    }
}
