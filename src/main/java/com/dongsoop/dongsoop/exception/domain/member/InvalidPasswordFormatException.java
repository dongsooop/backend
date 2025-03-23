package com.dongsoop.dongsoop.exception.domain.member;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidPasswordFormatException extends CustomException {

    public InvalidPasswordFormatException() {
        super("비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다.", HttpStatus.BAD_REQUEST);
    }

}