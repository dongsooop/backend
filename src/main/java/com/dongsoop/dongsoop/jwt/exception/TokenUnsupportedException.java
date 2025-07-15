package com.dongsoop.dongsoop.jwt.exception;

import org.springframework.http.HttpStatus;

public class TokenUnsupportedException extends JWTException {

    public TokenUnsupportedException(Exception e) {
        super("지원하지 않는 JWT 형식입니다.", HttpStatus.BAD_REQUEST, e);
    }

}
