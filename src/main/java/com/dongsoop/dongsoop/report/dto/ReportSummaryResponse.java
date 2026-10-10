package com.dongsoop.dongsoop.report.dto;

import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;

import java.time.LocalDateTime;

public record ReportSummaryResponse(
        Long id,
        String reporterNickname,
        ReportType reportType,
        ReportReason reportReason,
        Boolean isProcessed,
        LocalDateTime createdAt,
        Long targetMemberId,
        Long targetId,
        String description,
        String chatRoomId,
        String messageId,
        String messageContent,
        LocalDateTime messageSentAt,
        ChatMessageSnapshots messageContext
) implements ReportListItem {
}