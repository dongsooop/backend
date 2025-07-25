package com.dongsoop.dongsoop.mailverify.dto;

import jakarta.validation.constraints.Pattern;

public record MailSendRequest(

        @Pattern(regexp = "^[a-zA-Z0-9]+@dongyang.ac.kr$", message = "email 형식이 올바르지 않습니다. 특수 문자를 제외한 영문자와 숫자만 포함해야 하며, @dongyang.ac.kr로 끝나야 합니다.")
        String to
) {
}
