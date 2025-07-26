package com.dongsoop.dongsoop.mailverify.dto;

import com.dongsoop.dongsoop.email.annotation.SchoolEmail;
import jakarta.validation.constraints.NotBlank;

public record MailSendRequest(

        @SchoolEmail
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        String userEmail
) {
}
