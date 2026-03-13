package com.dongsoop.dongsoop.recaptcha.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ReCaptchaTokenNotFoundException extends CustomException {

    public ReCaptchaTokenNotFoundException() {
        super("reCAPTCHA 토큰이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}
