package com.dongsoop.dongsoop.exception.domain.member;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidEmailFormatException extends CustomException {

    public InvalidEmailFormatException() {
        super("이메일 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
    }

}