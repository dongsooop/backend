package com.dongsoop.dongsoop.recruitment.tutoring.dto;

import java.time.LocalDateTime;

public record TutoringBoardOverview(

        Long id,
        Integer volunteer,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String title,
        String content,
        String tags
) {
}
