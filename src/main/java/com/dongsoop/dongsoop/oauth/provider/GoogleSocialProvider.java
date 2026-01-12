package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.dto.SocialAccountLinkRequest;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccountId;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.AlreadyLinkedSocialAccountException;
import com.dongsoop.dongsoop.oauth.exception.InvalidGoogleTokenException;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import com.dongsoop.dongsoop.oauth.validator.MemberSocialAccountValidator;
import java.time.LocalDateTime;
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
    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final MemberRepository memberRepository;
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
    public Long login(String providerToken) {
        String providerId = this.getProviderId(providerToken);

        MemberSocialAccountDto socialAccount = memberSocialAccountValidator.validate(providerId);
        return socialAccount.member().getId();
    }

    @Override
    public LocalDateTime linkSocialAccount(Long memberId, SocialAccountLinkRequest request) {
        String providerId = this.getProviderId(request.providerToken());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        MemberSocialAccountId socialAccountId = new MemberSocialAccountId(providerId, OAuthProviderType.APPLE);
        if (this.memberSocialAccountRepository.existsById(socialAccountId)) {
            throw new AlreadyLinkedSocialAccountException();
        }

        MemberSocialAccount socialAccount = new MemberSocialAccount(socialAccountId, member);
        MemberSocialAccount saved = this.memberSocialAccountRepository.save(socialAccount);

        return saved.getCreateAt();
    }

    private String getProviderId(String providerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(providerToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    googleUserInfoUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = (Map<String, Object>) response.getBody();

            return body.get(googleUserNameAttribute).toString();
        } catch (HttpClientErrorException e) {
            throw new InvalidGoogleTokenException();
        }
    }
}
