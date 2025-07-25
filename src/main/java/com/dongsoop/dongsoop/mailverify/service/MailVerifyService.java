package com.dongsoop.dongsoop.mailverify.service;

import jakarta.mail.MessagingException;

public interface MailVerifyService {

    void sendMail(String to) throws MessagingException;
}
