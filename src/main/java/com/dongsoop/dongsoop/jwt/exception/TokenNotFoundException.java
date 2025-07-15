package com.dongsoop.dongsoop.jwt.exception;

import org.springframework.http.HttpStatus;

public class TokenNotFoundException extends JWTException {

    public TokenNotFoundException() {
        super("토큰이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
    }

}
