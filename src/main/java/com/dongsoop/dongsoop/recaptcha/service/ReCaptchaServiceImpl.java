package com.dongsoop.dongsoop.recaptcha.service;

import com.dongsoop.dongsoop.recaptcha.dto.ReCaptchaVerificationResponse;
import com.dongsoop.dongsoop.recaptcha.exception.ReCaptchaScoreTooLowException;
import com.dongsoop.dongsoop.recaptcha.exception.ReCaptchaVerificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class ReCaptchaServiceImpl implements ReCaptchaService {

    private final RestTemplate restTemplate;

    @Value("${recaptcha.secret-key}")
    private String secretKey;

    @Value("${recaptcha.verify-url}")
    private String verifyUrl;

    @Value("${recaptcha.score-threshold}")
    private double scoreThreshold;

    @Override
    public void verify(String token, String action) {
        ReCaptchaVerificationResponse response = requestVerification(token);

        if (!response.isSuccess()) {
            log.warn("reCAPTCHA verification failed. error-codes: {}", response.getErrorCodes());
            throw new ReCaptchaVerificationException();
        }

        if (StringUtils.hasText(action) && !action.equals(response.getAction())) {
            log.warn("reCAPTCHA action mismatch. expected: {}, actual: {}", action, response.getAction());
            throw new ReCaptchaVerificationException();
        }

        if (response.getScore() < scoreThreshold) {
            log.warn("reCAPTCHA score too low. score: {}, threshold: {}", response.getScore(), scoreThreshold);
            throw new ReCaptchaScoreTooLowException(response.getScore());
        }

        log.debug("reCAPTCHA verification successful. action: {}, score: {}", response.getAction(),
                response.getScore());
    }

    private ReCaptchaVerificationResponse requestVerification(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", secretKey);
        params.add("response", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ReCaptchaVerificationResponse response = restTemplate.postForObject(verifyUrl, request,
                ReCaptchaVerificationResponse.class);

        if (response == null) {
            log.error("reCAPTCHA verification response is null");
            throw new ReCaptchaVerificationException();
        }

        return response;
    }
}
