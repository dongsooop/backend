package com.dongsoop.dongsoop.jwt.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TokenMalformedException extends CustomException {

    public TokenMalformedException() {
        super("토큰이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
    }

    public TokenMalformedException(Exception e) {
        super("토큰이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED, e);
    }

}
