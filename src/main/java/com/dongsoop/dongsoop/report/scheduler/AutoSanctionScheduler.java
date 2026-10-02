package com.dongsoop.dongsoop.report.scheduler;

import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.AsyncAutoSanctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoSanctionScheduler {

    private final ReportRepository reportRepository;
    private final AsyncAutoSanctionService asyncAutoSanctionService;
    private final Set<String> processingTargets = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedRate = 3600000)
    public void processAutoSanctions() {
        log.info("Auto sanction scheduler started");

        Pageable pageable = PageRequest.of(0, 3);
        List<Report> reports = reportRepository.findUnprocessedReports(pageable);

        log.info("Unprocessed reports found: {}", reports.size());

        if (reports.isEmpty()) {
            log.info("No reports to process");
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        // 이미 끝난 처리는 whenComplete가 공유 집합에서 키를 바로 지우므로, 같은 배치 안 중복은 별도 집합으로 막는다
        Set<String> batchTargets = new HashSet<>();
        for (Report report : reports) {
            String targetKey = targetKeyOf(report);
            if (!batchTargets.add(targetKey) || !processingTargets.add(targetKey)) {
                continue;
            }

            CompletableFuture<Void> future = asyncAutoSanctionService.processReportAsync(report)
                    .whenComplete((result, throwable) -> processingTargets.remove(targetKey));
            futures.add(future);
        }

        log.info("Reports after duplicate filtering: {}", futures.size());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
            log.info("Auto sanction scheduler completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Auto sanction scheduler interrupted", e);
        } catch (TimeoutException e) {
            log.warn("Auto sanction processing timeout (exceeded 60 seconds)");
        } catch (Exception e) {
            log.error("Error occurred during auto sanction scheduler execution", e);
        }
    }

    private static String targetKeyOf(Report report) {
        if (report.getReportType() == ReportType.CHAT_MESSAGE) {
            return ReportType.CHAT_MESSAGE + ":" + report.getMessageId();
        }
        return report.getReportType() + ":" + report.getTargetId();
    }
}
