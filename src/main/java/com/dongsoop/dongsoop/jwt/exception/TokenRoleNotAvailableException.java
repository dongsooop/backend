package com.dongsoop.dongsoop.jwt.exception;

import org.springframework.http.HttpStatus;

public class TokenRoleNotAvailableException extends JWTException {

    public TokenRoleNotAvailableException() {
        super("토큰의 권한이 적절하지 않습니다.", HttpStatus.FORBIDDEN);
    }

}
