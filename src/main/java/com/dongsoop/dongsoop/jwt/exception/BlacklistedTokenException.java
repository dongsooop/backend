package com.dongsoop.dongsoop.jwt.exception;

public class BlacklistedTokenException extends JWTException {

    public BlacklistedTokenException() {
        super("만료된 세션입니다.");
    }
}
