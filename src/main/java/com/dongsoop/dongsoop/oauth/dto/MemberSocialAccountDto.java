package com.dongsoop.dongsoop.oauth.dto;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;

public record MemberSocialAccountDto(

        Member member,
        String providerId,
        OAuthProviderType providerType,
        List<RoleType> roleType
) {

    public boolean isMemberNotMatched() {
        return this.member == null;
    }

    public boolean isMemberDeleted() {
        return this.member.isDeleted();
    }
}
