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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoSanctionScheduler {

    private final ReportRepository reportRepository;
    private final AsyncAutoSanctionService asyncAutoSanctionService;

    @Scheduled(fixedRate = 60000)
    public void processAutoSanctions() {
        log.info("자동 제재 스케줄러 시작");

        Pageable pageable = PageRequest.of(0, 3);
        List<Report> reports = reportRepository.findUnprocessedReports(pageable);

        log.info("미처리 신고: {}개", reports.size());

        if (reports.isEmpty()) {
            log.info("처리할 신고가 없습니다");
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Report report : reports) {
            CompletableFuture<Void> future = asyncAutoSanctionService.processReportAsync(report);
            futures.add(future);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
            log.info("자동 제재 스케줄러 완료");
        } catch (TimeoutException e) {
            log.warn("자동 제재 처리 타임아웃 (60초 초과)");
        } catch (Exception e) {
            log.error("자동 제재 스케줄러 실행 중 오류", e);
        }
    }
}