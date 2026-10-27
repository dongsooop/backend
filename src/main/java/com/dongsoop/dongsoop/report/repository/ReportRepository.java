package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReportRepository extends JpaRepository<Report, Long>, ReportRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Report r WHERE r.id = :id")
    Optional<Report> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT r.targetMember.id FROM Report r WHERE r.id = :id")
    Optional<Long> findTargetMemberIdById(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM (SELECT pg_advisory_xact_lock(:memberId)) AS target_lock", nativeQuery = true)
    long lockTargetMember(@Param("memberId") Long memberId);

    /**
     * 같은 대상 회원의 신고 처리를 트랜잭션 단위로 직렬화한 뒤 신고 행을 잠근다. 같은 대상의 신고는 함께 닫히므로,
     * 신고 행끼리 서로 기다리는 교착 없이 늦게 온 처리는 이미 처리된 신고를 보게 된다.
     * 대상 ID는 스칼라로 읽는다. 엔티티를 먼저 읽으면 잠금 조회가 영속성 컨텍스트의 이전 상태를 돌려준다.
     */
    default Optional<Report> findByIdForUpdateLockingTarget(Long id) {
        findTargetMemberIdById(id).ifPresent(this::lockTargetMember);
        return findByIdForUpdate(id);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM Report r
            WHERE r.reportType IN :reportTypes
            AND r.targetMember.id = :targetMemberId
            AND r.isProcessed = false
            AND r.id <> :excludedId
            ORDER BY r.id
            """)
    List<Report> findUnprocessedSameTargetMessageReportsForUpdate(
            @Param("reportTypes") Collection<ReportType> reportTypes,
            @Param("targetMemberId") Long targetMemberId,
            @Param("excludedId") Long excludedId);

    /**
     * 관리자가 메시지 신고를 처리한 뒤, 같은 대상 회원의 다른 미처리 메시지 신고(채팅·과팅)를 같은 처리 주체로 함께 닫는다.
     * 게시판·회원 신고는 대상이 아니다. 호출 전에 {@link #findByIdForUpdateLockingTarget}로 대상 잠금을 잡아야 한다.
     */
    default void resolveSameTargetMessageReports(Report resolved) {
        if (!resolved.getReportType().isMessageReport() || resolved.getTargetMember() == null) {
            return;
        }

        findUnprocessedSameTargetMessageReportsForUpdate(ReportType.MESSAGE_TYPES,
                resolved.getTargetMember().getId(), resolved.getId())
                .forEach(report -> report.resolveWith(resolved));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM Report r
            WHERE r.reportType = :reportType
            AND r.chatRoomId = :chatRoomId
            AND r.messageId = :messageId
            AND r.isProcessed = false
            AND r.id <> :excludedId
            ORDER BY r.id
            """)
    List<Report> findUnprocessedSameMessageReportsForUpdate(@Param("reportType") ReportType reportType,
                                                           @Param("chatRoomId") String chatRoomId,
                                                           @Param("messageId") String messageId,
                                                           @Param("excludedId") Long excludedId);

    /**
     * 자동 판정은 그 메시지만 보았으므로, 같은 메시지의 다른 미처리 신고만 resolver로 함께 닫는다.
     * 같은 메시지는 대상 회원도 같아 {@link #findByIdForUpdateLockingTarget}의 대상 잠금으로 직렬화된다.
     */
    default void resolveSameMessageReports(Report processed, Report resolver) {
        findUnprocessedSameMessageReportsForUpdate(processed.getReportType(), processed.getChatRoomId(),
                processed.getMessageId(), processed.getId()).forEach(report -> report.resolveWith(resolver));
    }

    boolean existsByReporterAndReportTypeAndTargetId(Member reporter, ReportType reportType, Long targetId);

    boolean existsByReporterIdAndMessageId(Long reporterId, String messageId);

    boolean existsByReporterIdAndReportTypeAndChatRoomIdAndTargetMemberId(
            Long reporterId, ReportType reportType, String chatRoomId, Long targetMemberId);

    List<Report> findByReportTypeAndChatRoomIdAndMessageContextAfterIsNull(ReportType reportType, String chatRoomId);

    Optional<Report> findFirstByMessageIdAndSanctionSanctionTypeOrderByIdAsc(String messageId,
                                                                            SanctionType sanctionType);

    // 엔티티를 flush하면 모든 컬럼을 덮어써 동시에 커밋된 처리 상태(is_processed, sanction_id)를 되돌릴 수 있다.
    // 행마다 따로 커밋해 행 잠금을 하나만 잡는다. 여러 행을 잡고 있으면 함께 해결의 행 잠금과 서로 기다릴 수 있다
    @Transactional
    @Modifying
    @Query("UPDATE Report r SET r.messageContextAfter = :contextAfter WHERE r.id = :id AND r.messageContextAfter IS NULL")
    int recordContextAfter(@Param("id") Long id, @Param("contextAfter") ChatMessageSnapshots contextAfter);

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