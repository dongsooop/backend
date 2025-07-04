package com.dongsoop.dongsoop.member.repository;

import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import java.util.Optional;

public interface MemberRepositoryCustom {

    Optional<LoginMemberDetails> findLoginMemberDetailById(Long id);

    long softDelete(Long id, String emailAlias, String passwordAlias);
}
