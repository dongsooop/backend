package com.dongsoop.dongsoop.oauth.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidGoogleTokenException extends CustomException {

    public InvalidGoogleTokenException() {
        super("유효하지 않은 구글 토큰입니다.", HttpStatus.UNAUTHORIZED);
    }
}
