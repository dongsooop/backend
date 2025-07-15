package com.dongsoop.dongsoop.jwt.exception;

public class TokenSignatureException extends JWTException {

    public TokenSignatureException() {
        super("토큰의 서명이 올바르지 않습니다.");
    }

    public TokenSignatureException(Exception exception) {
        super("토큰의 서명이 올바르지 않습니다.", exception);
    }
}
