package com.dongsoop.dongsoop.report.dto;

import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.SanctionType;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        String reporterNickname,
        ReportType reportType,
        Long targetId,
        String targetUrl,
        ReportReason reportReason,
        String description,
        Boolean isProcessed,
        String adminNickname,
        String targetMemberNickname,
        SanctionType sanctionType,
        String sanctionReason,
        LocalDateTime sanctionStartAt,
        LocalDateTime sanctionEndAt,
        Boolean isSanctionActive,
        LocalDateTime createdAt
) {
}