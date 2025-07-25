package com.dongsoop.dongsoop.member.dto;

import com.dongsoop.dongsoop.email.annotation.SchoolEmail;
import jakarta.validation.constraints.NotBlank;

public record EmailValidateRequest(

        @SchoolEmail
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        String email
) {
}
