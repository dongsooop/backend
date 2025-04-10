package com.dongsoop.dongsoop.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {
    @Pattern(regexp = "^[a-zA-Z0-9]+@dongyang.ac.kr$", message = "email 형식이 올바르지 않습니다.")
    @NotBlank(message = "로그인 시 이메일은 필수입니다.")
    private String email;

    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[~!@#$%^&*()_\\-+=\\[\\]{}|\\\\;:'\"<>,.?/]).{8,20}$",
            message = "비밀번호 형식이 올바르지 않습니다.")
    @NotBlank(message = "로그인 시 비밀번호 입력은 필수입니다.")
    private String password;
}
