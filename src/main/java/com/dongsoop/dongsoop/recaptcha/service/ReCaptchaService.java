package com.dongsoop.dongsoop.recaptcha.service;

public interface ReCaptchaService {

    void verify(String token, String action);
}
