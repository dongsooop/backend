package com.dongsoop.dongsoop.eclass.scheduler;

import com.dongsoop.dongsoop.eclass.service.EclassSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EclassSyncScheduler {

    private final EclassSyncService eclassSyncService;

    @Scheduled(cron = "${eclass.sync.cron}", zone = "Asia/Seoul")
    public void sync() {
        log.info("eclass sync scheduler started");
        eclassSyncService.syncAll();
        log.info("eclass sync scheduler ended");
    }
}
