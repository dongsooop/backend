package com.dongsoop.dongsoop.member.repository;

import com.dongsoop.dongsoop.member.dto.PasswordValidateDto;
import com.dongsoop.dongsoop.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<PasswordValidateDto> findPasswordValidatorByEmail(String email);

    Optional<Member> findByEmail(String email);
}
