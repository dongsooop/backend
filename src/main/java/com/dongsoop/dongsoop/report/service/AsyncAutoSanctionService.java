package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.handler.ContentDeletionHandler;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AsyncAutoSanctionService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final String AUTO_SANCTION_REASON = "부적절한 언어 사용";
    private static final String AUTO_SANCTION_DESCRIPTION = "자동 제재에 의한 게시글 삭제";
    private static final String AUTO_WARNING_DESCRIPTION = "자동 제재에 의한 경고";
    private final SanctionRepository sanctionRepository;
    private final ContentDeletionHandler contentDeletionHandler;
    private final BoardContentService boardContentService;
    private final TextFilteringService textFilteringService;
    private final ReportRepository reportRepository;
    private final SanctionExecutor sanctionExecutor;
    @Value("${admin.id}")
    private Long systemAdminId;

    @Async("autoSanctionExecutor")
    public CompletableFuture<Void> processReportAsync(Report report) {
        try {
            log.info("Report processing started - Report ID: {}", report.getId());

            if (ReportType.CHAT_MESSAGE.equals(report.getReportType())) {
                judgeChatMessage(report);
            } else {
                checkProfanityAndExecute(report);
            }

            log.info("Auto sanction completed - Report ID: {}", report.getId());

        } catch (Exception e) {
            log.error("Auto sanction failed - Report ID: {}", report.getId(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    private void checkProfanityAndExecute(Report report) {
        String title = boardContentService.getTitle(report.getTargetId(), report.getReportType());
        String content = boardContentService.getContent(report.getTargetId(), report.getReportType());

        boolean hasProfanity = textFilteringService.hasProfanity(title, "", content);

        log.info("Profanity filtering result - Report ID: {}, HasProfanity: {}", report.getId(), hasProfanity);

        if (!hasProfanity) {
            log.info("No profanity detected - Report ID: {}", report.getId());
            report.markAsProcessedWithoutSanction();
            reportRepository.save(report);
            return;
        }

        executeSanction(report);
    }

    // 욕설이 아니거나 필터 호출이 실패하면 닫지 않는다. 스팸·사기 같은 사유는 욕설 필터로 판단할 수 없다
    private void judgeChatMessage(Report report) {
        boolean hasProfanity = textFilteringService.hasProfanity("", "", report.getMessageContent());
        log.info("Chat profanity result - Report ID: {}, HasProfanity: {}", report.getId(), hasProfanity);

        if (!hasProfanity) {
            report.markAutoReviewed();
            reportRepository.save(report);
            return;
        }

        Member systemAdmin = createSystemAdmin();
        Member targetMember = report.getTargetMember();
        LocalDateTime now = LocalDateTime.now(KST);
        Sanction warning = Sanction.builder()
                .member(targetMember)
                .admin(systemAdmin)
                .targetMember(targetMember)
                .report(report)
                .sanctionType(SanctionType.WARNING)
                .reason(AUTO_SANCTION_REASON)
                .startDate(now)
                .endDate(SanctionType.WARNING.resolveEndDate(null, now))
                .description(AUTO_WARNING_DESCRIPTION)
                .build();

        sanctionRepository.save(warning);
        report.processSanction(systemAdmin, targetMember, warning);
        reportRepository.save(report);
        sanctionExecutor.checkWarningAccumulation(targetMember);
    }

    private void executeSanction(Report report) {
        Member systemAdmin = createSystemAdmin();
        Sanction sanction = createSanction(report, systemAdmin);

        sanctionRepository.save(sanction);
        report.processSanction(systemAdmin, report.getTargetMember(), sanction);
        reportRepository.save(report);
        contentDeletionHandler.deleteContent(report);
    }

    private Member createSystemAdmin() {
        return Member.builder()
                .id(systemAdminId)
                .build();
    }

    private Sanction createSanction(Report report, Member systemAdmin) {
        return Sanction.builder()
                .member(report.getTargetMember())
                .admin(systemAdmin)
                .targetMember(report.getTargetMember())
                .report(report)
                .sanctionType(SanctionType.CONTENT_DELETION)
                .reason(AUTO_SANCTION_REASON)
                .startDate(LocalDateTime.now(KST))
                .endDate(LocalDateTime.now(KST).plusDays(1))
                .description(AUTO_SANCTION_DESCRIPTION)
                .build();
    }
}