package com.dongsoop.dongsoop.jwt.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TokenSignatureException extends CustomException {

    public TokenSignatureException() {
        super("토큰의 서명이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
    }

}
