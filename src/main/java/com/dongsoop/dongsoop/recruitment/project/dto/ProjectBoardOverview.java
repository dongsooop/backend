package com.dongsoop.dongsoop.recruitment.project.dto;

import java.time.LocalDateTime;

public record ProjectBoardOverview(

        Long id,
        Integer volunteer,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String title,
        String content,
        String tags
) {
}
