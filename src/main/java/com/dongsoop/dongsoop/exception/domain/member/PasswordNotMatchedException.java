package com.dongsoop.dongsoop.exception.domain.member;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class PasswordNotMatchedException extends CustomException {

    public PasswordNotMatchedException() {
        super("비밀번호가 일치 하지 않습니다.", HttpStatus.UNAUTHORIZED);
    }
}
