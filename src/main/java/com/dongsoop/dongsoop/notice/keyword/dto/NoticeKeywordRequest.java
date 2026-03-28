package com.dongsoop.dongsoop.notice.keyword.dto;

import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeywordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeKeywordRequest(

        @NotBlank
        @Size(max = 20)
        String keyword,

        @NotNull
        NoticeKeywordType type
) {
}
