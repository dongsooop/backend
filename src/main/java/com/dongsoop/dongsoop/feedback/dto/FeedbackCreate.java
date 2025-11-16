package com.dongsoop.dongsoop.feedback.dto;

import org.hibernate.validator.constraints.Length;

public record FeedbackCreate(

        @Length(max = 500, min = 5, message = "피드백 내용은 최소 5글자부터 최대 500자까지 입력할 수 있습니다.")
        String content
) {
}
