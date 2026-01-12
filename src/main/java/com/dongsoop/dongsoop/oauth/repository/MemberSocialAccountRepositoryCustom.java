package com.dongsoop.dongsoop.oauth.repository;

import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import java.util.Optional;

public interface MemberSocialAccountRepositoryCustom {

    Optional<MemberSocialAccountDto> findMemberSocialAccountDTO(String providerId);

    Optional<MemberSocialAccount> findByMemberIdAndProviderType(Long memberId,
                                                                OAuthProviderType providerType);
}
