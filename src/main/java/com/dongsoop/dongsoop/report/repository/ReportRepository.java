package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long>, ReportRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Report r WHERE r.id = :id")
    Optional<Report> findByIdForUpdate(@Param("id") Long id);

    // lock.timeout -2는 SKIP LOCKED다. 같은 메시지의 두 신고를 동시에 처리하면 서로의 행을 기다려 교착되므로,
    // 다른 처리가 잠근 신고는 건너뛰고 그 처리에 맡긴다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            SELECT r FROM Report r
            WHERE r.reportType = :reportType
            AND r.chatRoomId = :chatRoomId
            AND r.messageId = :messageId
            AND r.isProcessed = false
            AND r.id <> :excludedId
            """)
    List<Report> findUnprocessedSameMessageReportsForUpdate(@Param("reportType") ReportType reportType,
                                                           @Param("chatRoomId") String chatRoomId,
                                                           @Param("messageId") String messageId,
                                                           @Param("excludedId") Long excludedId);

    /**
     * 메시지 신고를 처리한 뒤, 같은 메시지의 다른 미처리 신고를 같은 처리 주체로 함께 닫는다. 게시판·회원 신고는 대상이 아니다.
     */
    default void resolveSameMessageReports(Report resolved) {
        if (!resolved.getReportType().isMessageReport()) {
            return;
        }

        findUnprocessedSameMessageReportsForUpdate(resolved.getReportType(), resolved.getChatRoomId(),
                resolved.getMessageId(), resolved.getId()).forEach(report -> report.resolveWith(resolved));
    }

    boolean existsByReporterAndReportTypeAndTargetId(Member reporter, ReportType reportType, Long targetId);

    boolean existsByReporterIdAndMessageId(Long reporterId, String messageId);

    boolean existsByReporterIdAndReportTypeAndChatRoomIdAndTargetMemberId(
            Long reporterId, ReportType reportType, String chatRoomId, Long targetMemberId);

    List<Report> findByReportTypeAndChatRoomIdAndMessageContextAfterIsNull(ReportType reportType, String chatRoomId);

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