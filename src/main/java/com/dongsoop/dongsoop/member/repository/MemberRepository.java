package com.dongsoop.dongsoop.member.repository;

import com.dongsoop.dongsoop.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, String> {
}
