package com.dongsoop.dongsoop.report.dto;
import com.dongsoop.dongsoop.report.entity.Sanction;

import java.time.LocalDateTime;
import java.util.Objects;

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
        String typeName = "UNKNOWN";
        if (sanction.getSanctionType() != null) {
            typeName = sanction.getSanctionType().name();
        }

        String reason = Objects.requireNonNullElse(sanction.getReason(), "");
        String description = Objects.requireNonNullElse(sanction.getDescription(), "");

        return new SanctionStatusResponse(
                true,
                typeName,
                reason,
                sanction.getStartDate(),
                sanction.getEndDate(),
                description
        );
    }
}