package com.dongsoop.dongsoop.oauth.service;

import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountOverview;
import com.dongsoop.dongsoop.oauth.dto.UnlinkSocialAccountRequest;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface OAuth2Service {

    LoginResponse acceptLogin(Authentication authentication, Long memberId);

    void unlinkMemberWithProviderType(Long memberId,
                                      OAuthProviderType providerType,
                                      UnlinkSocialAccountRequest request);

    void unlinkMember(Long memberId);

    List<MemberSocialAccountOverview> getSocialAccountState(Long memberId);
}
