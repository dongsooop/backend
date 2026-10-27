package com.dongsoop.dongsoop.jwt.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotRefreshTokenException extends CustomException {

    public NotRefreshTokenException() {
        super("리프레시 토큰이 아닙니다.", HttpStatus.UNAUTHORIZED);
    }
}
