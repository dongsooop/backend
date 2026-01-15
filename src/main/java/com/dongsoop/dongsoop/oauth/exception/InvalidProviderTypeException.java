package com.dongsoop.dongsoop.oauth.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidProviderTypeException extends CustomException {

    public InvalidProviderTypeException() {
        super("유효하지 않은 공급자 타입입니다.", HttpStatus.BAD_REQUEST);
    }

    public InvalidProviderTypeException(String providerType) {
        super("유효하지 않은 공급자 타입입니다: " + providerType, HttpStatus.BAD_REQUEST);
    }
}
