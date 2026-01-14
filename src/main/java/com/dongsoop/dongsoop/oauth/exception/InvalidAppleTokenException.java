package com.dongsoop.dongsoop.oauth.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidAppleTokenException extends CustomException {

    public InvalidAppleTokenException() {
        super("유효하지 않은 애플 토큰입니다.", HttpStatus.UNAUTHORIZED);
    }
}
