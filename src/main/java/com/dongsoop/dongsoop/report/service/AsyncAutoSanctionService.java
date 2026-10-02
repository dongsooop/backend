package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AsyncAutoSanctionService {

    private static final String AUTO_SANCTION_REASON = "부적절한 언어 사용";
    private static final String AUTO_SANCTION_DESCRIPTION = "자동 제재에 의한 게시글 삭제";
    private static final String AUTO_WARNING_DESCRIPTION = "자동 제재에 의한 경고";
    private final BoardContentService boardContentService;
    private final TextFilteringService textFilteringService;
    private final ReportRepository reportRepository;
    private final SanctionExecutor sanctionExecutor;

    @Async("autoSanctionExecutor")
    public CompletableFuture<Void> processReportAsync(Report detachedReport) {
        Long reportId = detachedReport.getId();
        try {
            log.info("Report processing started - Report ID: {}", reportId);

            Report report = reportRepository.findById(reportId).orElse(null);
            if (report == null || report.getIsProcessed()) {
                log.info("Report already processed or removed - Report ID: {}", reportId);
                return CompletableFuture.completedFuture(null);
            }

            if (ReportType.CHAT_MESSAGE.equals(report.getReportType())) {
                judgeChatMessage(report);
            } else {
                checkProfanityAndExecute(report);
            }

            log.info("Auto sanction completed - Report ID: {}", reportId);

        } catch (Exception e) {
            log.error("Auto sanction failed - Report ID: {}", reportId, e);
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
            return;
        }

        sanctionExecutor.issueBySystem(report, SanctionType.CONTENT_DELETION, AUTO_SANCTION_REASON,
                AUTO_SANCTION_DESCRIPTION);
    }

    // 욕설이 아니거나 필터 호출이 실패하면 닫지 않는다. 스팸·사기 같은 사유는 욕설 필터로 판단할 수 없다
    private void judgeChatMessage(Report report) {
        boolean hasProfanity = textFilteringService.hasProfanity("", "", report.getMessageContent());
        log.info("Chat profanity result - Report ID: {}, HasProfanity: {}", report.getId(), hasProfanity);

        if (!hasProfanity) {
            report.markAutoReviewed();
            return;
        }

        if (reportRepository.existsByMessageIdAndSanctionSanctionType(report.getMessageId(), SanctionType.WARNING)) {
            log.info("Message already warned - Report ID: {}, Message ID: {}", report.getId(), report.getMessageId());
            report.markAsProcessedWithoutSanction();
            return;
        }

        sanctionExecutor.issueBySystem(report, SanctionType.WARNING, AUTO_SANCTION_REASON, AUTO_WARNING_DESCRIPTION);
    }
}
