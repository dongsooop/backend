package com.dongsoop.dongsoop.mailverify.service;

public interface MailVerifyService {

    void sendPasswordChangeMail(String userEmail);

    void sendMail(String to);

    void validateVerificationCode(String email, String code);
}
