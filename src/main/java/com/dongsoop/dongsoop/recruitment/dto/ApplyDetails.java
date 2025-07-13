package com.dongsoop.dongsoop.recruitment.dto;

import java.time.LocalDateTime;

public record ApplyDetails(

        Long boardId,
        Long applierId,
        String applierName,
        String departmentName,
        LocalDateTime applyTime,
        String introduction,
        String motivation
) {
}
