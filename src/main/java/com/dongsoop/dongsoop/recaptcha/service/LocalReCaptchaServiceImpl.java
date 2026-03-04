package com.dongsoop.dongsoop.recaptcha.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("local")
public class LocalReCaptchaServiceImpl implements ReCaptchaService {

    @Override
    public void verify(String token, String action) {
        log.debug("reCAPTCHA verification skipped in local profile. action: {}", action);
    }
}
