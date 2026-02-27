package com.dongsoop.dongsoop.oauth.service;

import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountOverview;
import com.dongsoop.dongsoop.oauth.dto.UnlinkSocialAccountRequest;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.InvalidProviderTypeException;
import com.dongsoop.dongsoop.oauth.provider.OAuth2UserParser;
import com.dongsoop.dongsoop.oauth.provider.SocialProvider;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final OAuth2UserParser oAuth2UserParser;
    private final MemberRepository memberRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final TokenGenerator tokenGenerator;

    @Override
    public LoginResponse acceptLogin(Authentication authentication, Long memberId, String deviceToken) {
        Long deviceId = resolveDeviceId(memberId, deviceToken);
        String accessToken = tokenGenerator.generateAccessToken(authentication, deviceId);
        String refreshToken = tokenGenerator.generateRefreshToken(authentication, deviceId);

        LoginMemberDetails loginMemberDetails = memberRepository.findLoginMemberDetailById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        return new LoginResponse(loginMemberDetails, accessToken, refreshToken);
    }

    private Long resolveDeviceId(Long memberId, String deviceToken) {
        if (!StringUtils.hasText(deviceToken)) {
            return null;
        }
        return memberDeviceRepository.findByMemberIdAndDeviceToken(memberId, deviceToken)
                .map(MemberDevice::getId)
                .orElse(null);
    }

    @Override
    @Transactional
    public void unlinkMemberWithProviderType(Long memberId,
                                             OAuthProviderType providerType,
                                             UnlinkSocialAccountRequest request) {
        memberSocialAccountRepository.findByMemberIdAndProviderType(memberId, providerType)
                .ifPresent(memberSocialAccountRepository::delete);

        SocialProvider socialProvider = oAuth2UserParser.extractProvider(providerType.name().toLowerCase());
        if (socialProvider == null) {
            throw new InvalidProviderTypeException();
        }
        socialProvider.revoke(request.token());
    }

    @Override
    @Transactional
    public void unlinkMember(Long memberId) {
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
