package com.dongsoop.dongsoop.report.dto;

import com.dongsoop.dongsoop.report.entity.Sanction;

import java.time.LocalDateTime;

public record SanctionStatusResponse(
        boolean isSanctioned,
        String sanctionType,
        String reason,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String description
) {

    public static SanctionStatusResponse noSanction() {
        return new SanctionStatusResponse(false, null, null, null, null, null);
    }

    public static SanctionStatusResponse withSanction(Sanction sanction) {
        return new SanctionStatusResponse(
                true,
                sanction.getSanctionType().name(),
                sanction.getReason(),
                sanction.getStartDate(),
                sanction.getEndDate(),
                sanction.getDescription()
        );
    }
}