package com.dongsoop.dongsoop.oauth.repository;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountOverview;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import java.util.List;
import java.util.Optional;

public interface MemberSocialAccountRepositoryCustom {

    Optional<MemberSocialAccountDto> findMemberSocialAccountDTO(String providerId, OAuthProviderType providerType);

    Optional<MemberSocialAccount> findByMemberIdAndProviderType(Long memberId,
                                                                OAuthProviderType providerType);

    List<MemberSocialAccountOverview> findAllMemberSocialAccountOverview(Long memberId);

    Optional<MemberSocialAccount> findByMemberAndProviderType(Member member, OAuthProviderType providerType);
}
