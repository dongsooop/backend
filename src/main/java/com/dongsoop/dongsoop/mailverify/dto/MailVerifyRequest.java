package com.dongsoop.dongsoop.mailverify.dto;

import jakarta.validation.constraints.Pattern;

public record MailVerifyRequest(

        @Pattern(regexp = "^[a-zA-Z0-9]+@dongyang.ac.kr$", message = "email 형식이 올바르지 않습니다. 특수 문자를 제외한 영문자와 숫자만 포함해야 하며, @dongyang.ac.kr로 끝나야 합니다.")
        String to,

        @Pattern(regexp = "^[A-Z0-9]{6}$", message = "인증 코드는 대문자, 숫자로 이루어진 6자리여야 합니다.")
        String code
) {
}
