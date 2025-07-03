package com.dongsoop.dongsoop.member.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class PasswordNotMatchedException extends CustomException {

    public PasswordNotMatchedException() {
        super("비밀번호가 일치 하지 않습니다.", HttpStatus.UNAUTHORIZED);
    }
}
