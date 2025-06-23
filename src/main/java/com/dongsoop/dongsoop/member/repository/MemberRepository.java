package com.dongsoop.dongsoop.member.repository;

import com.dongsoop.dongsoop.member.dto.LoginAuthenticate;
import com.dongsoop.dongsoop.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<LoginAuthenticate> findLoginAuthenticateByEmail(String email);

    boolean existsByEmail(String email);

    Optional<LoginAuthenticate> findLoginAuthenticateByNickname(String nickname);

    boolean existsByNickname(String nickname);
}
