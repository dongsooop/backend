package com.dongsoop.dongsoop.jwt.exception;

public class TokenExpiredException extends JWTException {

    public TokenExpiredException() {
        super("토큰이 만료되었습니다.");
    }

    public TokenExpiredException(Exception e) {
        super("토큰이 만료되었습니다.", e);
    }

}
