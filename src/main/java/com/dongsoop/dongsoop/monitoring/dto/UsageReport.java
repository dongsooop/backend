package com.dongsoop.dongsoop.monitoring.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record UsageReport(
        LocalDate from,
        LocalDate to,
        long users,
        Integer usersDeltaPercent,
        long memberUsers,
        long calls,
        Integer callsDeltaPercent,
        double p95Ms,
        Map<LocalDate, Long> dailyUsers,
        List<FeatureUsage> features,
        List<String> newFeatures,
        List<String> droppedFeatures,
        List<UsageWindow.EndpointCount> topByCalls,
        List<UsageWindow.EndpointLatency> slowest,
        long serverErrors,
        List<UsageWindow.EndpointCount> errorsByUri,
        UsageWindow.HourCount peakHour
) {

    /** deltaPercent 가 null 이면 전주 기록이 없는 신규 항목 */
    public record FeatureUsage(String feature, long users, long calls, Integer usersDeltaPercent) {
    }
}
