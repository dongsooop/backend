package com.dongsoop.dongsoop.report.dto;

import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record CreateServerMessageReportRequest(
        @NotNull(message = "신고 유형은 필수입니다.")
        ReportType reportType,

        @NotNull(message = "신고자 ID는 필수입니다.")
        Long reporterId,

        @NotNull(message = "신고 대상 회원 ID는 필수입니다.")
        Long targetMemberId,

        @NotBlank(message = "채팅방 ID는 필수입니다.")
        @Size(max = 64, message = "채팅방 ID는 64자 이하여야 합니다.")
        String roomId,

        @NotBlank(message = "메시지 ID는 필수입니다.")
        @Size(max = 64, message = "메시지 ID는 64자 이하여야 합니다.")
        String messageId,

        @NotNull(message = "메시지 내용은 필수입니다.")
        String messageContent,

        @NotNull(message = "메시지 전송 시각은 필수입니다.")
        LocalDateTime messageSentAt,

        List<@Valid @NotNull ContextMessage> context,

        @NotNull(message = "신고 사유는 필수입니다.")
        ReportReason reason,

        @Size(max = 500, message = "신고 내용은 500자 이하로 입력해주세요.")
        String description
) {

    public record ContextMessage(
            @NotNull(message = "맥락 메시지 발신자 ID는 필수입니다.")
            Long senderId,

            @NotNull(message = "맥락 메시지 내용은 필수입니다.")
            String content,

            @NotNull(message = "맥락 메시지 전송 시각은 필수입니다.")
            LocalDateTime sentAt
    ) {
    }
}
