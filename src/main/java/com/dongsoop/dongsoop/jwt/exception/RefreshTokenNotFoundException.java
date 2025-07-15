package com.dongsoop.dongsoop.jwt.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenNotFoundException extends JWTException {

    public RefreshTokenNotFoundException() {
        super("리프레시 토큰이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}
