package com.dongsoop.dongsoop.jwt.exception;

public class NotAccessTokenException extends JWTException {

    public NotAccessTokenException() {
        super("액세스 토큰이 아닙니다.");
    }
}
