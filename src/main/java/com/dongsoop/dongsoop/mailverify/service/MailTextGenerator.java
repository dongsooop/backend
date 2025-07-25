package com.dongsoop.dongsoop.mailverify.service;

public interface MailTextGenerator {

    String generateVerificationText(String code);
}
