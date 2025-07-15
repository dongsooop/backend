package com.dongsoop.dongsoop.jwt.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class JWTException extends CustomException {

    public JWTException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public JWTException(String message, HttpStatus httpStatus, Exception cause) {
        super(message, httpStatus, cause);
    }

    public JWTException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }

    public JWTException(String message, Exception cause) {
        super(message, HttpStatus.UNAUTHORIZED, cause);
    }
}
