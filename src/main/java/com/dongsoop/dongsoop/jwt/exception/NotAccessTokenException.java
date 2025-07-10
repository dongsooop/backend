package com.dongsoop.dongsoop.jwt.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotAccessTokenException extends CustomException {

    public NotAccessTokenException() {
        super("액세스 토큰이 아닙니다.", HttpStatus.UNAUTHORIZED);
    }
}
