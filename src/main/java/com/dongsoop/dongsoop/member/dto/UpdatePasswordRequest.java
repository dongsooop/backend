package com.dongsoop.dongsoop.member.dto;

import com.dongsoop.dongsoop.email.annotation.SchoolEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdatePasswordRequest(

        @SchoolEmail
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        String email,

        @NotNull
        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
                message = "비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다.")
        String password,

        @Pattern(regexp = "^[A-Z0-9]{6}$", message = "인증 코드는 대문자, 숫자로 이루어진 6자리여야 합니다.")
        @NotBlank(message = "인증 코드는 필수 입력값입니다.")
        String code
) {
}
