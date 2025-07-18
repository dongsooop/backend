package com.dongsoop.dongsoop.report.dto;

import com.dongsoop.dongsoop.report.entity.Sanction;

import com.dongsoop.dongsoop.report.entity.SanctionType;
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
        String sanctionTypeName = extractSafeTypeName(sanction);
        String sanctionReason = extractSafeReason(sanction);
        String sanctionDescription = extractSafeDescription(sanction);

        return new SanctionStatusResponse(
                true,
                sanctionTypeName,
                sanctionReason,
                sanction.getStartDate(),
                sanction.getEndDate(),
                sanctionDescription
        );
    }

    private static String extractSafeTypeName(Sanction sanction) {
        SanctionType type = sanction.getSanctionType();

        if (type == null) {
            return "UNKNOWN";
        }
        return type.name();
    }

    private static String extractSafeReason(Sanction sanction) {
        String reason = sanction.getReason();

        if (reason == null) {
            return "";
        }
        return reason;
    }

    private static String extractSafeDescription(Sanction sanction) {
        String description = sanction.getDescription();

        if (description == null) {
            return "";
        }
        return description;
    }
}