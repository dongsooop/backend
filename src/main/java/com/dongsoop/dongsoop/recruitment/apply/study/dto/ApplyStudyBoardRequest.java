package com.dongsoop.dongsoop.recruitment.apply.study.dto;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record ApplyStudyBoardRequest(

        @NotNull
        Long boardId,

        @Length(max = 500)
        String introduction,

        @Length(max = 500)
        String motivation
) {
}
