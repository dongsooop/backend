package com.dongsoop.dongsoop.monitoring.dto;

import java.util.List;
import java.util.Map;

/** ES 집계 한 번의 결과. 한 기간(예: 지난 7일)의 원시 수치. */
public record UsageWindow(
        long calls,
        long users,
        Map<String, FeatureCount> features,
        List<EndpointLatency> slowest,
        long serverErrors,
        String topErrorUri
) {

    public static UsageWindow empty() {
        return new UsageWindow(0, 0, Map.of(), List.of(), 0, null);
    }

    public record FeatureCount(long calls, long users) {
    }

    public record EndpointLatency(String uri, double p95Ms) {
    }
}
