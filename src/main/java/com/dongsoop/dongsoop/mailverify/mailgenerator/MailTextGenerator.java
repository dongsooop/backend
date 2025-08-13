package com.dongsoop.dongsoop.mailverify.mailgenerator;

public interface MailTextGenerator {

    String generateVerificationText(String code);
}
