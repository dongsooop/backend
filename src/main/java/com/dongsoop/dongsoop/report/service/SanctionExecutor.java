package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.handler.ContentDeletionHandler;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SanctionExecutor {

    private static final long WARNING_THRESHOLD = 3L;

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ContentDeletionHandler contentDeletionHandler;

    @Transactional
    public void executeSanction(Report report) {
        getSanctionExecutors()
                .get(report.getSanctionType())
                .accept(report);
    }

    private Map<SanctionType, Consumer<Report>> getSanctionExecutors() {
        return Map.of(
                SanctionType.WARNING, this::executeWarning,
                SanctionType.TEMPORARY_BAN, this::executeTemporaryBan,
                SanctionType.PERMANENT_BAN, this::executePermanentBan,
                SanctionType.CONTENT_DELETION, this::executeContentDeletion
        );
    }

    private void executeWarning(Report report) {
        log.info("경고 제재 실행: {}", report.getId());
        checkWarningAccumulation(report.getTargetMember());
    }

    private void executeTemporaryBan(Report report) {
        log.info("일시정지 제재 실행: {}", report.getId());
    }

    private void executePermanentBan(Report report) {
        log.info("영구정지 제재 실행: {}", report.getId());
    }

    private void executeContentDeletion(Report report) {
        contentDeletionHandler.deleteContent(report);
        log.info("게시글 삭제 제재 실행: {}", report.getId());
    }

    private void checkWarningAccumulation(Member member) {
        Long warningCount = reportRepository.countActiveWarningsForMember(member.getId());
        executeAutoSuspensionWhen(warningCount, member);
    }

    private void executeAutoSuspensionWhen(Long warningCount, Member member) {
        if (warningCount >= WARNING_THRESHOLD) {
            log.info("경고 {}회 누적으로 인한 자동 일시정지 실행: {}", WARNING_THRESHOLD, member.getId());
            createAutoSuspensionReport(member.getId());
        }
    }

    private void createAutoSuspensionReport(Long memberId) {
        Member memberRef = memberRepository.getReferenceById(memberId);

        Report autoSuspensionReport = buildAutoSuspensionReport(memberRef);
        reportRepository.save(autoSuspensionReport);
        log.info("자동 일시정지 제재 생성 완료: 회원 ID {}", memberId);
    }

    private Report buildAutoSuspensionReport(Member member) {
        return Report.builder()
                .reporter(member)
                .reportType(ReportType.MEMBER)
                .targetId(member.getId())
                .reportReason(ReportReason.OTHER)
                .description("경고 3회 누적으로 인한 자동 일시정지")
                .targetUrl("/api/members/" + member.getId())
                .admin(member)
                .targetMember(member)
                .sanctionType(SanctionType.TEMPORARY_BAN)
                .sanctionReason("경고 3회 누적으로 인한 자동 일시정지")
                .sanctionStartAt(LocalDateTime.now())
                .sanctionEndAt(LocalDateTime.now().plusDays(7))
                .isProcessed(true)
                .isSanctionActive(true)
                .build();
    }
}