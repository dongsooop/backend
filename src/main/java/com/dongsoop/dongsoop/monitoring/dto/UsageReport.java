package com.dongsoop.dongsoop.monitoring.dto;

import java.time.LocalDate;
import java.util.List;

public record UsageReport(
        LocalDate from,
        LocalDate to,
        long users,
        Integer usersDeltaPercent,
        long calls,
        Integer callsDeltaPercent,
        List<FeatureUsage> features,
        UsageWindow.EndpointLatency slowest,
        long serverErrors,
        String topErrorUri
) {

    /** deltaPercent 가 null 이면 전주 기록이 없는 신규 항목 */
    public record FeatureUsage(String feature, long users, long calls, Integer usersDeltaPercent) {
    }
}
