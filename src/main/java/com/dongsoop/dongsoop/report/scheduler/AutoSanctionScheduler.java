package com.dongsoop.dongsoop.report.scheduler;

import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.AsyncAutoSanctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

        List<Report> filteredReports = reports.stream()
                .filter(this::isNotAlreadyProcessing)
                .toList();

        log.info("Reports after duplicate filtering: {}", filteredReports.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Report report : filteredReports) {
            String targetKey = report.getReportType() + ":" + report.getTargetId();
            processingTargets.add(targetKey);

            CompletableFuture<Void> future = asyncAutoSanctionService.processReportAsync(report)
                    .whenComplete((result, throwable) -> processingTargets.remove(targetKey));
            futures.add(future);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
            log.info("Auto sanction scheduler completed");
        } catch (TimeoutException e) {
            log.warn("Auto sanction processing timeout (exceeded 60 seconds)");
        } catch (Exception e) {
            log.error("Error occurred during auto sanction scheduler execution", e);
        }
    }

    private boolean isNotAlreadyProcessing(Report report) {
        String targetKey = report.getReportType() + ":" + report.getTargetId();
        return !processingTargets.contains(targetKey);
    }
}