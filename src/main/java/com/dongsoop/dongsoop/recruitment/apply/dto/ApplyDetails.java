package com.dongsoop.dongsoop.recruitment.apply.dto;

import java.time.LocalDateTime;

public record ApplyDetails(

        Long boardId,
        String title,
        Long applierId,
        String applierName,
        String departmentName,
        LocalDateTime applyTime,
        String introduction,
        String motivation
) {
}
