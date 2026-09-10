package com.dongsoop.dongsoop.eclass.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class EclassInvalidTokenException extends CustomException {

    public EclassInvalidTokenException() {
        super("이클래스 토큰이 유효하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}
