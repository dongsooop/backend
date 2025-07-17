package com.dongsoop.dongsoop.report.dto;

import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull(message = "신고 대상 타입은 필수입니다.")
        ReportType reportType,

        @NotNull(message = "신고 대상 ID는 필수입니다.")
        @Positive(message = "신고 대상 ID는 양수여야 합니다.")
        Long targetId,

        @NotNull(message = "신고 사유는 필수입니다.")
        ReportReason reason,

        @Size(max = 500, message = "신고 내용은 500자 이하로 입력해주세요.")
        String description,

        @Positive(message = "신고 대상 회원 ID는 양수여야 합니다.")
        Long targetMemberId
) {
}