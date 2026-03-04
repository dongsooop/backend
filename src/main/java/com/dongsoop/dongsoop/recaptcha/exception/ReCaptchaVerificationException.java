package com.dongsoop.dongsoop.recaptcha.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ReCaptchaVerificationException extends CustomException {

    public ReCaptchaVerificationException() {
        super("reCAPTCHA 검증에 실패했습니다.", HttpStatus.FORBIDDEN);
    }
}
