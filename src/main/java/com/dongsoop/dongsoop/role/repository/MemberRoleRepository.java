package com.dongsoop.dongsoop.role.repository;

import com.dongsoop.dongsoop.role.entity.MemberRole;
import com.dongsoop.dongsoop.role.entity.MemberRole.MemberRoleKey;
import com.dongsoop.dongsoop.role.entity.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRoleRepository extends JpaRepository<MemberRole, MemberRoleKey> {

    @Query("SELECT mr.id.role FROM MemberRole mr WHERE mr.id.member.id = :memberId")
    List<Role> findAllByMemberId(@Param("memberId") Long memberId);
}
