package com.dongsoop.dongsoop.jwt.exception;

public class NotRefreshTokenException extends JWTException {

    public NotRefreshTokenException() {
        super("리프레시 토큰이 아닙니다.");
    }
}
