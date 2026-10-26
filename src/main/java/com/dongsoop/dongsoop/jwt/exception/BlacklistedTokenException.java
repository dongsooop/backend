package com.dongsoop.dongsoop.jwt.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class BlacklistedTokenException extends CustomException {

    public BlacklistedTokenException() {
        super("만료된 세션입니다.", HttpStatus.UNAUTHORIZED);
    }
}
