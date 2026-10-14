package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.report.entity.Sanction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SanctionRepository extends JpaRepository<Sanction, Long> {

    @Query("SELECT s FROM Sanction s WHERE s.member.id = :memberId AND s.isActive = true")
    List<Sanction> findActiveSanctionsByMemberId(@Param("memberId") Long memberId);
}
