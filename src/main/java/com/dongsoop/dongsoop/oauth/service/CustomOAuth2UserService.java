package com.dongsoop.dongsoop.oauth.service;

import com.dongsoop.dongsoop.oauth.dto.CustomOAuth2User;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.exception.AccountNotLinkedException;
import com.dongsoop.dongsoop.oauth.exception.LinkedAccountAlreadyDeletedException;
import com.dongsoop.dongsoop.oauth.provider.OAuth2UserParser;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
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

    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final ReportValidator reportValidator;
    private final OAuth2UserParser oAuth2UserParser;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 소셜 로그인 API를 통해 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 소셜 계정에 따라 id 추출
        String registrationId = userRequest.getClientRegistration()
                .getRegistrationId();
        String providerId = this.oAuth2UserParser.extractProviderId(oAuth2User, registrationId);

        // 소셜 계정과 연동된 회원이 있는지 확인
        MemberSocialAccountDto socialAccount = memberSocialAccountRepository.findMemberSocialAccountDTO(providerId)
                .orElseThrow(() -> {
                    log.info("social account not linked: providerId={}", providerId);
                    return new AccountNotLinkedException();
                });

        // 소셜 계정이 회원과 매칭되지 않은 경우 예외 처리
        if (socialAccount.isMemberNotMatched()) {
            log.info("social account not linked: providerId={}", providerId);
            throw new AccountNotLinkedException();
        }

        // 소셜 계정과 연결된 계정이 탈퇴된 경우 예외 처리
        if (socialAccount.isMemberDeleted()) {
            log.info("linked account was deleted: providerId={} memberId={}", providerId,
                    socialAccount.member().getId());
            throw new LinkedAccountAlreadyDeletedException();
        }

        // 신고당한 회원인지 검증
        reportValidator.checkMemberAccessById(socialAccount.member().getId());

        return new CustomOAuth2User(oAuth2User, socialAccount.member().getId(), socialAccount.roleType());
    }
}
