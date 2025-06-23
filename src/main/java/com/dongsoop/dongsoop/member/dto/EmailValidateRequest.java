package com.dongsoop.dongsoop.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailValidateRequest(

        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Pattern(regexp = "[A-Za-z0-9]+@dongyang.ac.kr$", message = "이메일은 동양미래대학교 이메일 형식이어야 합니다.")
        String email
) {
}
