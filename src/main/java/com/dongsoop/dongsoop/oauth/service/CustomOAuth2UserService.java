package com.dongsoop.dongsoop.oauth.service;

import com.dongsoop.dongsoop.oauth.dto.CustomOAuth2User;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.provider.OAuth2UserParser;
import com.dongsoop.dongsoop.oauth.provider.SocialProvider;
import com.dongsoop.dongsoop.oauth.validator.MemberSocialAccountValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberSocialAccountValidator memberSocialAccountValidator;
    private final OAuth2UserParser oAuth2UserParser;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 소셜 로그인 API를 통해 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 소셜 계정에 따라 id 추출
        String registrationId = userRequest.getClientRegistration()
                .getRegistrationId();
        SocialProvider provider = this.oAuth2UserParser.extractProvider(registrationId);
        if (provider == null) {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        String providerId = provider.extractProviderId(oAuth2User, registrationId);

        try {
            OAuthProviderType oAuthProviderType = OAuthProviderType.valueOf(registrationId.toUpperCase());

            MemberSocialAccountDto socialAccount = memberSocialAccountValidator.validate(providerId, oAuthProviderType);

            return new CustomOAuth2User(oAuth2User, socialAccount.member().getId(), socialAccount.roleType());
        } catch (IllegalArgumentException e) {
            log.error("Unsupported OAuth provider: {}", registrationId);
            throw new OAuth2AuthenticationException("Unsupported OAuth provider: " + registrationId);
        }
    }
}
