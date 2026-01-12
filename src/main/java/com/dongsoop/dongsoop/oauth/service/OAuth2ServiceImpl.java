package com.dongsoop.dongsoop.oauth.service;

import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountOverview;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final MemberRepository memberRepository;
    private final TokenGenerator tokenGenerator;

    @Override
    public LoginResponse acceptLogin(Long memberId) {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        String accessToken = tokenGenerator.generateAccessToken(authentication);
        String refreshToken = tokenGenerator.generateRefreshToken(authentication);

        LoginMemberDetails loginMemberDetails = memberRepository.findLoginMemberDetailById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        return new LoginResponse(loginMemberDetails, accessToken, refreshToken);
    }

    public void withdrawMemberWithProviderType(Long memberId, OAuthProviderType providerType) {
        memberSocialAccountRepository.findByMemberIdAndProviderType(memberId, providerType)
                .ifPresent(memberSocialAccountRepository::delete);
    }

    public void withdrawMember(Long memberId) {
        List<MemberSocialAccount> socialAccountList = this.memberSocialAccountRepository.findByMemberId(memberId);
        if (socialAccountList.isEmpty()) {
            return;
        }

        this.memberSocialAccountRepository.deleteAll(socialAccountList);
    }

    @Override
    public List<MemberSocialAccountOverview> getSocialAccountState(Long memberId) {
        return this.memberSocialAccountRepository.findAllMemberSocialAccountOverview(
                memberId);
    }
}
