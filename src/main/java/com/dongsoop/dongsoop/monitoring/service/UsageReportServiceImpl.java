package com.dongsoop.dongsoop.monitoring.service;

import com.dongsoop.dongsoop.monitoring.client.UsageStatsClient;
import com.dongsoop.dongsoop.monitoring.dto.UsageReport;
import com.dongsoop.dongsoop.monitoring.dto.UsageReport.FeatureUsage;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.FeatureCount;
import com.dongsoop.dongsoop.monitoring.interceptor.ApiUsageInterceptor;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "monitoring.usage.enabled", havingValue = "true")
public class UsageReportServiceImpl implements UsageReportService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final UsageStatsClient statsClient;

    @Override
    public UsageReport build(LocalDate from, LocalDate to) throws IOException {
        Period length = Period.between(from, to);
        UsageWindow current = statsClient.query(from.atStartOfDay(ZONE), to.atStartOfDay(ZONE));
        UsageWindow previous = statsClient.query(from.minus(length).atStartOfDay(ZONE), from.atStartOfDay(ZONE));
        return compare(from, to.minusDays(1), current, previous);
    }

    public static UsageReport compare(LocalDate from, LocalDate to, UsageWindow current, UsageWindow previous) {
        List<FeatureUsage> features = current.features().entrySet().stream()
                .map(entry -> {
                    FeatureCount now = entry.getValue();
                    FeatureCount before = previous.features().get(entry.getKey());
                    return new FeatureUsage(entry.getKey(), now.users(), now.calls(),
                            deltaPercent(now.users(), before == null ? 0 : before.users()));
                })
                .sorted(Comparator.comparingLong(FeatureUsage::users).reversed()
                        .thenComparing(Comparator.comparingLong(FeatureUsage::calls).reversed()))
                .toList();

        // 전주엔 사용자가 없었는데 이번 주 생긴 기능 / 전주엔 있었는데 이번 주 0명이 된 기능.
        // 전주 기록이 아예 없으면(첫 주) 전부 신규라 의미가 없어 비운다
        List<String> newFeatures = previous.users() == 0 ? List.of() : features.stream()
                .filter(f -> f.users() > 0 && f.usersDeltaPercent() == null)
                .map(FeatureUsage::feature)
                .filter(name -> !ApiUsageInterceptor.SYSTEM_FEATURE.equals(name))
                .toList();
        List<String> droppedFeatures = previous.features().entrySet().stream()
                .filter(e -> e.getValue().users() > 0)
                .map(java.util.Map.Entry::getKey)
                .filter(name -> !ApiUsageInterceptor.SYSTEM_FEATURE.equals(name))
                .filter(name -> {
                    FeatureCount now = current.features().get(name);
                    return now == null || now.users() == 0;
                })
                .sorted()
                .toList();

        return new UsageReport(
                from, to,
                current.users(), deltaPercent(current.users(), previous.users()),
                current.memberUsers(),
                current.calls(), deltaPercent(current.calls(), previous.calls()),
                current.p95Ms(),
                current.dailyUsers(),
                features, newFeatures, droppedFeatures,
                current.topByCalls(),
                current.slowest(),
                current.serverErrors(),
                current.errorsByUri(),
                current.peakHour()
        );
    }

    /** 전주가 0이면 비교할 수 없으므로 null(신규). 둘 다 0이면 변동 없음 */
    public static Integer deltaPercent(long now, long before) {
        if (before == 0) {
            return now == 0 ? 0 : null;
        }
        return (int) Math.round((now - before) * 100.0 / before);
    }
}
