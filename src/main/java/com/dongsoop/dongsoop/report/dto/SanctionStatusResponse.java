package com.dongsoop.dongsoop.report.dto;

import java.time.LocalDateTime;

public record SanctionStatusResponse(
        boolean isSanctioned,
        String sanctionType,
        String reason,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String description
) {
}
