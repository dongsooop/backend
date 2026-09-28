package com.dongsoop.dongsoop.report.dto;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.ReportType;
import java.time.LocalDateTime;

public record MessageReportDraft(ReportType reportType, String chatRoomId, String targetUrl,
                                 Member targetMember, String messageId, String messageContent,
                                 LocalDateTime messageSentAt, ChatMessageSnapshots messageContext) {
}
