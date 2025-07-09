package com.dongsoop.dongsoop.recruitment.dto;

import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateApplyStatusRequest(

        @NotNull
        @Positive
        Long applierId,

        @NotNull
        RecruitmentApplyStatus status
) {
    public boolean compareStatus(RecruitmentApplyStatus status) {
        return this.status == status;
    }
}
