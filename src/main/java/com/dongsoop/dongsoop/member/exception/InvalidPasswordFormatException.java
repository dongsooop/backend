package com.dongsoop.dongsoop.member.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidPasswordFormatException extends CustomException {

    public InvalidPasswordFormatException() {
        super("비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다.", HttpStatus.BAD_REQUEST);
    }

}
