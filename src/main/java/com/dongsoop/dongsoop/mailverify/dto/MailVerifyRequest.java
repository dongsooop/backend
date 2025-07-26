package com.dongsoop.dongsoop.mailverify.dto;

import com.dongsoop.dongsoop.email.annotation.SchoolEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MailVerifyRequest(

        @SchoolEmail
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        String userEmail,

        @Pattern(regexp = "^[A-Z0-9]{6}$", message = "인증 코드는 대문자, 숫자로 이루어진 6자리여야 합니다.")
        @NotBlank(message = "인증 코드는 필수 입력값입니다.")
        String code
) {
}
