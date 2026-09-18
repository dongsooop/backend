package com.dongsoop.dongsoop.monitoring.scheduler;

import com.dongsoop.dongsoop.monitoring.client.DiscordWebhookClient;
import com.dongsoop.dongsoop.monitoring.dto.UsageReport;
import com.dongsoop.dongsoop.monitoring.service.UsageReportService;
import com.dongsoop.dongsoop.monitoring.util.UsageReportMessage;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@ConditionalOnProperty(name = "monitoring.usage.enabled", havingValue = "true")
public class UsageReportScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final int REPORT_DAYS = 7;

    private final UsageReportService reportService;
    private final DiscordWebhookClient discordWebhookClient;
    private final String webhookUrl;

    public UsageReportScheduler(UsageReportService reportService,
                                DiscordWebhookClient discordWebhookClient,
                                @Value("${monitoring.discord.webhook-url:}") String webhookUrl) {
        this.reportService = reportService;
        this.discordWebhookClient = discordWebhookClient;
        this.webhookUrl = webhookUrl;
    }

    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Seoul")
    public void sendWeeklyReport() {
        sendReportEndingAt(LocalDate.now(ZONE));
    }

    /** [today-7, today) 기간의 리포트. 웹훅이 없으면 조회조차 하지 않고, 조회에 실패하면 보내지 않는다. */
    public void sendReportEndingAt(LocalDate today) {
        if (!StringUtils.hasText(webhookUrl)) {
            log.debug("Usage report webhook is not configured; skipping");
            return;
        }
        UsageReport report;
        try {
            report = reportService.build(today.minusDays(REPORT_DAYS), today);
        } catch (Exception e) {
            log.warn("Usage report query failed; skipping this week: {}", e.getMessage());
            return;
        }
        try {
            discordWebhookClient.send(webhookUrl, UsageReportMessage.format(report));
            log.info("Usage report sent ({} ~ {})", report.from(), report.to());
        } catch (Exception e) {
            log.warn("Usage report webhook failed: {}", e.getMessage());
        }
    }
}
