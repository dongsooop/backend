package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long>, ReportRepositoryCustom {

    boolean existsByReporterAndReportTypeAndTargetId(Member reporter, ReportType reportType, Long targetId);

    @Query("""
            SELECT COUNT(r) 
            FROM Report r 
            WHERE r.targetMember.id = :memberId 
            AND r.sanctionType = 'WARNING'
            AND r.isSanctionActive = true
            """)
    Long countActiveWarningsForMember(@Param("memberId") Long memberId);

    @Query("""
            SELECT r 
            FROM Report r 
            WHERE r.targetMember.id = :memberId 
            AND r.isSanctionActive = true 
            AND r.sanctionType IN ('TEMPORARY_BAN', 'PERMANENT_BAN')
            AND (r.sanctionEndAt IS NULL OR r.sanctionEndAt > :currentTime)
            ORDER BY r.createdAt DESC
            """)
    Optional<Report> findActiveBanForMember(@Param("memberId") Long memberId, @Param("currentTime") LocalDateTime currentTime);

}