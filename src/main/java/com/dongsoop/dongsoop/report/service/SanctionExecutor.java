package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.report.entity.*;
import com.dongsoop.dongsoop.report.handler.ContentDeletionHandler;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
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
    private static final String AUTO_SUSPENSION_REASON = "경고 3회 누적으로 인한 자동 일시정지";
    private static final String AUTO_SUSPENSION_DESCRIPTION = "경고 3회 누적으로 인한 자동 일시정지";
    private static final int AUTO_SUSPENSION_DAYS = 7;

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ContentDeletionHandler contentDeletionHandler;
    private final SanctionRepository sanctionRepository;

    @Transactional
    public void executeSanction(Report report) {
        SanctionType sanctionType = report.getSanction().getSanctionType();
        getSanctionExecutors()
                .getOrDefault(sanctionType, this::handleUnsupportedSanctionType)
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

    private void handleUnsupportedSanctionType(Report report) {
        SanctionType sanctionType = report.getSanction().getSanctionType();
        log.error("지원되지 않는 제재 타입: {}, 신고 ID: {}", sanctionType, report.getId());
        throw new IllegalArgumentException("지원되지 않는 제재 타입: " + sanctionType);
    }

    private void checkWarningAccumulation(Member member) {
        Long warningCount = reportRepository.countActiveWarningsForMember(
                member.getId(),
                SanctionType.WARNING
        );
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
        Sanction sanction = createAutoSuspensionSanction(memberRef);
        sanctionRepository.save(sanction);

        Report autoSuspensionReport = buildAutoSuspensionReport(memberRef, sanction);
        reportRepository.save(autoSuspensionReport);
        log.info("자동 일시정지 제재 생성 완료: 회원 ID {}", memberId);
    }

    private Report buildAutoSuspensionReport(Member member, Sanction sanction) {
        return Report.builder()
                .reporter(member)
                .reportType(ReportType.MEMBER)
                .targetId(member.getId())
                .reportReason(ReportReason.OTHER)
                .description(AUTO_SUSPENSION_DESCRIPTION)
                .targetUrl("/member/" + member.getId())
                .admin(member)
                .targetMember(member)
                .sanction(sanction)
                .isProcessed(true)
                .build();
    }

    private Sanction createAutoSuspensionSanction(Member member) {
        return Sanction.builder()
                .member(member)
                .sanctionType(SanctionType.TEMPORARY_BAN)
                .reason(AUTO_SUSPENSION_REASON)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(AUTO_SUSPENSION_DAYS))
                .description(AUTO_SUSPENSION_DESCRIPTION)
                .build();
    }
}