package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.report.entity.Sanction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SanctionRepository extends JpaRepository<Sanction, Long> {

    @Query("SELECT s FROM Sanction s WHERE s.member.id = :memberId AND s.isActive = true")
    Optional<Sanction> findActiveSanctionByMemberId(@Param("memberId") Long memberId);
}