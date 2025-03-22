package com.dongsoop.dongsoop.exception.domain.jwt;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TokenNotFoundException extends CustomException {

    public TokenNotFoundException() {
        super("토큰이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
    }

}
