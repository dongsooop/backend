package com.dongsoop.dongsoop.member.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record NicknameValidateRequest(

        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        @Length(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하로 입력해야 합니다.")
        String nickname
) {
}
