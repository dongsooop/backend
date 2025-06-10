package com.dongsoop.dongsoop.recruitment.project.dto;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record ApplyProjectBoardRequest(

        @NotNull
        Long boardId,

        @Length(max = 500)
        String introduction,

        @Length(max = 500)
        String motivation
) {
}
