package com.dongsoop.dongsoop.member.dto;

import com.dongsoop.dongsoop.email.annotation.SchoolEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @SchoolEmail
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    private String email;

    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[~!@#$%^&*()_\\-+=\\[\\]{}|\\\\;:'\"<>,.?/]).{8,20}$",
            message = "비밀번호 형식이 올바르지 않습니다. 영문, 숫자, 특수문자를 포함해야 하며 8자 이상 20자 이하여야 합니다.")
    @NotBlank(message = "로그인 시 비밀번호 입력은 필수입니다.")
    private String password;

    private String fcmToken;
}
