package com.dongsoop.dongsoop.recruitment.apply.dto;

import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import java.time.LocalDateTime;

public record ApplyDetails(

        Long boardId,
        String title,
        Long applierId,
        String applierName,
        String departmentName,
        LocalDateTime applyTime,
        String introduction,
        String motivation,
        RecruitmentApplyStatus status
) {
}
