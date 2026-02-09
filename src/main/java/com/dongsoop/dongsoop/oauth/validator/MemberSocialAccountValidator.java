package com.dongsoop.dongsoop.oauth.validator;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccountId;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;

public interface MemberSocialAccountValidator {

    MemberSocialAccountDto validate(String providerId, OAuthProviderType providerType);

    void validateAlreadyLinked(MemberSocialAccountId socialAccountId, Member member, OAuthProviderType providerType);
}
