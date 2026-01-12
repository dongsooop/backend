package com.dongsoop.dongsoop.oauth.repository;

import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import java.util.Optional;

public interface MemberSocialAccountRepositoryCustom {

    Optional<MemberSocialAccountDto> findMemberSocialAccountDTO(String providerId);
}
