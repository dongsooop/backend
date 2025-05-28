package com.dongsoop.dongsoop.exception.domain.jwt;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class RefreshTokenNotFoundException extends CustomException {

    public RefreshTokenNotFoundException() {
        super("리프레시 토큰이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}
