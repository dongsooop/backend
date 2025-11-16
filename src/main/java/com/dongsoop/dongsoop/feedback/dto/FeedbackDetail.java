package com.dongsoop.dongsoop.feedback.dto;

import java.time.LocalDateTime;

public record FeedbackDetail(

        Long id,
        String content,
        Long memberId,
        String memberNickname,
        LocalDateTime createdAt
) {
}
