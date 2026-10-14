package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.chat.service.ChatParticipantService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.report.entity.*;
import com.dongsoop.dongsoop.report.handler.ContentDeletionHandler;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class SanctionExecutor {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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
    private final ChatParticipantService chatParticipantService;
    @Value("${admin.id}")
    private Long systemAdminId;

    @Transactional
    public Sanction issue(Report report, Member admin, Member targetMember, SanctionType type,
                          String reason, LocalDateTime requestedEndAt, String description) {
        return issueInternal(report, admin, targetMember, type, reason, requestedEndAt, description);
    }

    @Transactional
    public Sanction issueBySystem(Report report, SanctionType type, String reason, String description) {
        return issueInternal(report, systemAdmin(), report.getTargetMember(), type, reason, null, description);
    }

    private Sanction issueInternal(Report report, Member admin, Member targetMember, SanctionType type,
                                   String reason, LocalDateTime requestedEndAt, String description) {
        report.ensureNotProcessed();

        LocalDateTime now = LocalDateTime.now(KST);
        Sanction sanction = Sanction.builder()
                .member(targetMember)
                .admin(admin)
                .targetMember(targetMember)
                .report(report)
                .sanctionType(type)
                .reason(Objects.requireNonNullElse(reason, type.getDescription()))
                .startDate(now)
                .endDate(type.resolveEndDate(requestedEndAt, now))
                .description(Objects.requireNonNullElse(description, type.getDescription()))
                .build();

        // 효과 실행(Redis 추방 등)보다 먼저 flush해 DB 제약 오류가 먼저 드러나게 한다
        sanctionRepository.saveAndFlush(sanction);
        report.processSanction(admin, targetMember, sanction);
        executeSanction(report);
        return sanction;
    }

    private Member systemAdmin() {
        return memberRepository.getReferenceById(systemAdminId);
    }

    private void executeSanction(Report report) {
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
                SanctionType.CONTENT_DELETION, this::executeContentDeletion,
                SanctionType.CHAT_KICK, this::executeChatKick
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

    private void executeChatKick(Report report) {
        log.info("채팅방 추방 제재 실행: {}", report.getId());
        chatParticipantService.kickUserByAdmin(report.getChatRoomId(), report.getTargetMember().getId());
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
            createAutoSuspension(member, SUSPENSION_DAYS_7, AUTO_SUSPENSION_DESCRIPTION_7);
            return;
        }

        if (warningCount == WARNING_THRESHOLD_5) {
            createAutoSuspension(member, SUSPENSION_DAYS_5, AUTO_SUSPENSION_DESCRIPTION_5);
            return;
        }

        if (warningCount == WARNING_THRESHOLD_3) {
            createAutoSuspension(member, SUSPENSION_DAYS_3, AUTO_SUSPENSION_DESCRIPTION_3);
        }
    }

    private void createAutoSuspension(Member member, int suspensionDays, String description) {
        log.info("{} 실행: {}", description, member.getId());

        // Sanction.report가 NOT NULL이라 신고를 먼저 저장하고 제재를 연결한다
        Report report = reportRepository.save(buildAutoSuspensionReport(member, description));
        issueInternal(report, systemAdmin(), member, SanctionType.TEMPORARY_BAN, description,
                LocalDateTime.now(KST).plusDays(suspensionDays), description);
        log.info("{} 생성 완료: 회원 ID {}", description, member.getId());
    }

    private Report buildAutoSuspensionReport(Member member, String description) {
        return Report.builder()
                .reporter(member)
                .reportType(ReportType.MEMBER)
                .targetId(member.getId())
                .reportReason(ReportReason.OTHER)
                .description(description)
                .targetUrl("/member/" + member.getId())
                .targetMember(member)
                .build();
    }
}
