package com.dongsoop.dongsoop.exception.domain.jwt;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TokenUnsupportedException extends CustomException {

    public TokenUnsupportedException(Exception e) {
        super("지원하지 않는 JWT 형식입니다.", HttpStatus.BAD_REQUEST, e);
    }

}
