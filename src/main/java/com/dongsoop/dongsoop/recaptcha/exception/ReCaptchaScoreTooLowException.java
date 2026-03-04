package com.dongsoop.dongsoop.recaptcha.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ReCaptchaScoreTooLowException extends CustomException {

    public ReCaptchaScoreTooLowException(double score) {
        super(String.format("reCAPTCHA 점수가 너무 낮습니다. (score: %.2f)", score), HttpStatus.FORBIDDEN);
    }
}
