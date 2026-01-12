package com.dongsoop.dongsoop.oauth.service;

import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountOverview;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import java.util.List;

public interface OAuth2Service {

    LoginResponse acceptLogin(Long memberId);

    void withdrawMemberWithProviderType(Long memberId, OAuthProviderType providerType);

    void withdrawMember(Long memberId);

    List<MemberSocialAccountOverview> getSocialAccountState(Long memberId);
}
