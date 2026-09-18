package com.dongsoop.dongsoop.monitoring.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** ES 집계 한 번의 결과. 한 기간(예: 지난 7일)의 원시 수치. */
public record UsageWindow(
        long calls,
        long users,
        long memberUsers,
        double p95Ms,
        Map<String, FeatureCount> features,
        Map<LocalDate, Long> dailyUsers,
        List<EndpointCount> topByCalls,
        List<EndpointLatency> slowest,
        long serverErrors,
        List<EndpointCount> errorsByUri,
        HourCount peakHour
) {

    public static UsageWindow empty() {
        return new UsageWindow(0, 0, 0, 0, Map.of(), Map.of(), List.of(), List.of(), 0, List.of(), null);
    }

    public record FeatureCount(long calls, long users) {
    }

    public record EndpointCount(String uri, long count) {
    }

    public record EndpointLatency(String uri, double p95Ms) {
    }

    /** 요일(1=월 … 7=일)과 시(0~23) 기준 가장 호출이 많았던 한 시간 */
    public record HourCount(int dayOfWeek, int hour, long count) {
    }
}
