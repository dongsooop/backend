package com.dongsoop.dongsoop.report.dto;

import com.dongsoop.dongsoop.report.entity.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateChatReportRequest(
        @NotBlank(message = "채팅방 ID는 필수입니다.")
        String roomId,

        @NotBlank(message = "메시지 ID는 필수입니다.")
        String messageId,

        @NotNull(message = "신고 사유는 필수입니다.")
        ReportReason reason,

        @Size(max = 500, message = "신고 내용은 500자 이하로 입력해주세요.")
        String description
) {
}
