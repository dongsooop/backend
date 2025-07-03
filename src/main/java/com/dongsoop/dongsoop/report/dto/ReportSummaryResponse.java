package com.dongsoop.dongsoop.report.dto;

import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;

import java.time.LocalDateTime;

public record ReportSummaryResponse(
        Long id,
        String reporterNickname,
        ReportType reportType,
        ReportReason reportReason,
        Boolean isProcessed,
        LocalDateTime createdAt
) {
}