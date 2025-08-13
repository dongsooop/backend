package com.dongsoop.dongsoop.mailverify.service;

public interface MailVerifyNumberGenerator {

    String generateVerificationCode(int length);

    String getRedisKeyByHashed(String redisPrefix, String userEmail);
}
