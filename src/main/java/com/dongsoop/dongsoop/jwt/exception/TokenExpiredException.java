package com.dongsoop.dongsoop.jwt.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TokenExpiredException extends CustomException {

    public TokenExpiredException() {
        super("토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED);
    }

    public TokenExpiredException(Exception e) {
        super("토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED, e);
    }

}
