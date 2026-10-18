package com.dongsoop.dongsoop.member.repository;

import com.dongsoop.dongsoop.member.dto.DeleteMember;
import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import java.util.Optional;

public interface MemberRepositoryCustom {

    Optional<LoginMemberDetails> findLoginMemberDetailById(Long id);

    long softDelete(DeleteMember deleteMember);

}
