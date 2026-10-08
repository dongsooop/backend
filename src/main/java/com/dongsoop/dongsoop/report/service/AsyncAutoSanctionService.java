package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncAutoSanctionService {

    private static final String AUTO_SANCTION_REASON = "부적절한 언어 사용";
    private static final String AUTO_SANCTION_DESCRIPTION = "자동 제재에 의한 게시글 삭제";
    private static final String AUTO_WARNING_DESCRIPTION = "자동 제재에 의한 경고";
    private final BoardContentService boardContentService;
    private final TextFilteringService textFilteringService;
    private final ReportRepository reportRepository;
    private final SanctionExecutor sanctionExecutor;
    private final TransactionTemplate transactionTemplate;

    // 필터 HTTP 호출은 트랜잭션 밖에서 해 DB 커넥션과 행 잠금을 잡지 않고, 결과 반영만 잠금 조회 후 한 트랜잭션에서 한다
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

            boolean hasProfanity = hasProfanity(report);
            log.info("Profanity filtering result - Report ID: {}, HasProfanity: {}", reportId, hasProfanity);

            transactionTemplate.executeWithoutResult(status -> applyResult(reportId, hasProfanity));

            log.info("Auto sanction completed - Report ID: {}", reportId);

        } catch (Exception e) {
            log.error("Auto sanction failed - Report ID: {}", reportId, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    private boolean hasProfanity(Report report) {
        if (ReportType.CHAT_MESSAGE.equals(report.getReportType())) {
            return textFilteringService.hasProfanity("", "", report.getMessageContent());
        }

        String title = boardContentService.getTitle(report.getTargetId(), report.getReportType());
        String content = boardContentService.getContent(report.getTargetId(), report.getReportType());
        return textFilteringService.hasProfanity(title, "", content);
    }

    // 필터를 기다리는 동안 관리자가 제재·기각했을 수 있어 잠금 조회로 다시 확인한다
    private void applyResult(Long reportId, boolean hasProfanity) {
        Report report = reportRepository.findByIdForUpdate(reportId).orElse(null);
        if (report == null || report.getIsProcessed()) {
            log.info("Report processed during filtering - Report ID: {}", reportId);
            return;
        }

        if (ReportType.CHAT_MESSAGE.equals(report.getReportType())) {
            judgeChatMessage(report, hasProfanity);
        } else {
            judgeBoard(report, hasProfanity);
        }
    }

    private void judgeBoard(Report report, boolean hasProfanity) {
        if (!hasProfanity) {
            log.info("No profanity detected - Report ID: {}", report.getId());
            report.markAsProcessedWithoutSanction();
            return;
        }

        sanctionExecutor.issueBySystem(report, SanctionType.CONTENT_DELETION, AUTO_SANCTION_REASON,
                AUTO_SANCTION_DESCRIPTION);
    }

    // 욕설이 아니거나 필터 호출이 실패하면 닫지 않는다. 스팸·사기 같은 사유는 욕설 필터로 판단할 수 없다
    private void judgeChatMessage(Report report, boolean hasProfanity) {
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
