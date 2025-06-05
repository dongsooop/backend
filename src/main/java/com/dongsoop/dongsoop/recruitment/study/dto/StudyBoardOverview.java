package com.dongsoop.dongsoop.recruitment.study.dto;

import java.time.LocalDateTime;

public record StudyBoardOverview(

        Long id,
        Integer volunteer,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String title,
        String content,
        String tags
) {
}
