package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long>, ReportRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Report r WHERE r.id = :id")
    Optional<Report> findByIdForUpdate(@Param("id") Long id);

    boolean existsByReporterAndReportTypeAndTargetId(Member reporter, ReportType reportType, Long targetId);

    boolean existsByReporterIdAndMessageId(Long reporterId, String messageId);

    boolean existsByReporterIdAndReportTypeAndChatRoomIdAndTargetMemberId(
            Long reporterId, ReportType reportType, String chatRoomId, Long targetMemberId);

    boolean existsByMessageIdAndSanctionSanctionType(String messageId, SanctionType sanctionType);

    @Query("""
            SELECT COUNT(r)
            FROM Report r
            JOIN r.sanction s
            WHERE r.targetMember.id = :memberId
            AND s.sanctionType = :sanctionType
            AND s.isActive = true
            """)
    Long countActiveWarningsForMember(@Param("memberId") Long memberId,
                                      @Param("sanctionType") SanctionType sanctionType);
}