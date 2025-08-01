package com.dongsoop.dongsoop.notice.service;

import org.springframework.scheduling.annotation.Scheduled;

public interface NoticeScheduler {

    @Scheduled(cron = "0 0 10,14,18 * * *", zone = "Asia/Seoul")
    void scheduled();
}
