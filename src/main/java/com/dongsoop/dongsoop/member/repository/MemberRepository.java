package com.dongsoop.dongsoop.member.repository;

import com.dongsoop.dongsoop.member.dto.LoginAuthenticate;
import com.dongsoop.dongsoop.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    Optional<LoginAuthenticate> findLoginAuthenticateByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.email = :email AND m.isDeleted = false")
    boolean existsByEmailAndIsDeletedFalse(@Param("email") String email);

    Optional<Member> findByEmail(String email);
}
