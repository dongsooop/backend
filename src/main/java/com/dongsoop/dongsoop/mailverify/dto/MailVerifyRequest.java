package com.dongsoop.dongsoop.mailverify.dto;

public record MailVerifyRequest(
        
        String to,
        String code
) {
}
