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

    // 주의 누적 단계별 상수
    private static final long WARNING_THRESHOLD_3 = 3L;
    private static final long WARNING_THRESHOLD_5 = 5L;
    private static final long WARNING_THRESHOLD_7 = 7L;

    // 정지 기간 상수
    private static final int SUSPENSION_DAYS_3 = 3;
    private static final int SUSPENSION_DAYS_5 = 14;
    private static final int SUSPENSION_DAYS_7 = 30;

    // 제재 사유 상수
    private static final String AUTO_SUSPENSION_DESCRIPTION_3 = "경고 3회 누적으로 인한 자동 3일 정지";
    private static final String AUTO_SUSPENSION_DESCRIPTION_5 = "경고 5회 누적으로 인한 자동 14일 정지";
    private static final String AUTO_SUSPENSION_DESCRIPTION_7 = "경고 7회 누적으로 인한 자동 30일 정지";

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

        contentDeletionHandler.deleteContent(report);
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

        log.info("회원 {} 주의 누적 횟수: {}회", member.getId(), warningCount);
        executeAutoSuspensionWhen(warningCount, member);
    }

    private void executeAutoSuspensionWhen(Long warningCount, Member member) {
        if (warningCount == WARNING_THRESHOLD_7) {
            createAutoSuspension30Days(member.getId());
            return;
        }

        if (warningCount == WARNING_THRESHOLD_5) {
            createAutoSuspension14Days(member.getId());
            return;
        }

        if (warningCount == WARNING_THRESHOLD_3) {
            createAutoSuspension3Days(member.getId());
        }
    }

    private void createAutoSuspension3Days(Long memberId) {
        log.info("경고 3회 누적으로 인한 자동 3일 정지 실행: {}", memberId);
        Member memberRef = memberRepository.getReferenceById(memberId);
        Sanction sanction = createAutoSuspensionSanction(memberRef, SUSPENSION_DAYS_3,
                AUTO_SUSPENSION_DESCRIPTION_3, AUTO_SUSPENSION_DESCRIPTION_3);
        sanctionRepository.save(sanction);

        Report autoSuspensionReport = buildAutoSuspensionReport(memberRef, sanction, AUTO_SUSPENSION_DESCRIPTION_3);
        reportRepository.save(autoSuspensionReport);
        log.info("자동 3일 정지 제재 생성 완료: 회원 ID {}", memberId);
    }

    private void createAutoSuspension14Days(Long memberId) {
        log.info("경고 5회 누적으로 인한 자동 14일 정지 실행: {}", memberId);
        Member memberRef = memberRepository.getReferenceById(memberId);
        Sanction sanction = createAutoSuspensionSanction(memberRef, SUSPENSION_DAYS_5,
                AUTO_SUSPENSION_DESCRIPTION_5, AUTO_SUSPENSION_DESCRIPTION_5);
        sanctionRepository.save(sanction);

        Report autoSuspensionReport = buildAutoSuspensionReport(memberRef, sanction, AUTO_SUSPENSION_DESCRIPTION_5);
        reportRepository.save(autoSuspensionReport);
        log.info("자동 14일 정지 제재 생성 완료: 회원 ID {}", memberId);
    }

    private void createAutoSuspension30Days(Long memberId) {
        log.info("경고 7회 누적으로 인한 자동 30일 정지 실행: {}", memberId);
        Member memberRef = memberRepository.getReferenceById(memberId);
        Sanction sanction = createAutoSuspensionSanction(memberRef, SUSPENSION_DAYS_7,
                AUTO_SUSPENSION_DESCRIPTION_7, AUTO_SUSPENSION_DESCRIPTION_7);
        sanctionRepository.save(sanction);

        Report autoSuspensionReport = buildAutoSuspensionReport(memberRef, sanction, AUTO_SUSPENSION_DESCRIPTION_7);
        reportRepository.save(autoSuspensionReport);
        log.info("자동 30일 정지 제재 생성 완료: 회원 ID {}", memberId);
    }

    private Report buildAutoSuspensionReport(Member member, Sanction sanction, String description) {
        return Report.builder()
                .reporter(member)
                .reportType(ReportType.MEMBER)
                .targetId(member.getId())
                .reportReason(ReportReason.OTHER)
                .description(description)
                .targetUrl("/member/" + member.getId())
                .admin(member)
                .targetMember(member)
                .sanction(sanction)
                .isProcessed(true)
                .build();
    }

    private Sanction createAutoSuspensionSanction(Member member, int suspensionDays, String reason, String description) {
        return Sanction.builder()
                .member(member)
                .sanctionType(SanctionType.TEMPORARY_BAN)
                .reason(reason)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(suspensionDays))
                .description(description)
                .build();
    }
}