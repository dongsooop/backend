package com.dongsoop.dongsoop.mailverify.service;

public interface MailVerifyService {

    void sendMail(String to);

    void validateVerificationCode(String email, String code);
}
