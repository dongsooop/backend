package com.dongsoop.dongsoop.jwt.exception;

public class TokenMalformedException extends JWTException {

    public TokenMalformedException() {
        super("토큰이 올바르지 않습니다.");
    }

    public TokenMalformedException(Exception e) {
        super("토큰이 올바르지 않습니다.", e);
    }

}
