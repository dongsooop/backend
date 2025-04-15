package com.dongsoop.dongsoop.exception.domain.jwt;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TokenMalformedException extends CustomException {

    public TokenMalformedException() {
        super("토큰이 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
    }

    public TokenMalformedException(Exception e) {
        super("토큰이 올바르지 않습니다.", HttpStatus.BAD_REQUEST, e);
    }

}
