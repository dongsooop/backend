package com.dongsoop.dongsoop.mailverify.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UnknownEmailEncodeException extends CustomException {

    public UnknownEmailEncodeException(Exception e) {
        super("인코딩 중 알 수 없는 예외가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
}
