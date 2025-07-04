package com.dongsoop.dongsoop.member.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidEmailFormatException extends CustomException {

    public InvalidEmailFormatException() {
        super("이메일 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
    }

}
