package com.dongsoop.dongsoop.report.dto;

import com.dongsoop.dongsoop.report.entity.SanctionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ProcessSanctionRequest(
        @NotNull(message = "신고 ID는 필수입니다.")
        @Positive(message = "신고 ID는 양수여야 합니다.")
        Long reportId,

        @NotNull(message = "제재 대상 회원 ID는 필수입니다.")
        @Positive(message = "제재 대상 회원 ID는 양수여야 합니다.")
        Long targetMemberId,

        @NotNull(message = "제재 타입은 필수입니다.")
        SanctionType sanctionType,

        @Size(max = 500, message = "제재 사유는 500자 이하로 입력해주세요.")
        String sanctionReason,

        LocalDateTime sanctionEndAt
) {
}