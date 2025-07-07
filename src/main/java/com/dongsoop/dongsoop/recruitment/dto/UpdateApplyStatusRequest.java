package com.dongsoop.dongsoop.recruitment.dto;

import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplyStatusRequest(

        @NotNull
        RecruitmentApplyStatus status
) {
}
