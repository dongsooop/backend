package com.dongsoop.dongsoop.mailverify.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UnknownMailMessagingException extends CustomException {

    public UnknownMailMessagingException(Exception e) {
        super("알 수 없는 메일 메시징 예외가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
}
