package com.dongsoop.dongsoop.monitoring.dto;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record ApiUsageEvent(
        ZonedDateTime timestamp,
        String method,
        String uri,
        String feature,
        int status,
        long durationMs,
        Long memberId,
        Long deviceId,
        String actor
) {

    public Map<String, Object> toDocument() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("@timestamp", timestamp.toOffsetDateTime().toString());
        doc.put("method", method);
        doc.put("uri", uri);
        doc.put("feature", feature);
        doc.put("status", status);
        doc.put("durationMs", durationMs);
        if (memberId != null) {
            doc.put("memberId", memberId);
        }
        if (deviceId != null) {
            doc.put("deviceId", deviceId);
        }
        doc.put("actor", actor);
        return doc;
    }
}
