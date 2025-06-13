package com.dongsoop.dongsoop.exception.domain.authentication;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotAuthenticationException extends CustomException {

    public NotAuthenticationException() {
        super("사용자의 인증 정보가 존재하지 않습니다.", HttpStatus.UNAUTHORIZED);
    }
}
