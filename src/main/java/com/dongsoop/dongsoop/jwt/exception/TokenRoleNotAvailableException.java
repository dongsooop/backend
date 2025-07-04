package com.dongsoop.dongsoop.jwt.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TokenRoleNotAvailableException extends CustomException {

    public TokenRoleNotAvailableException() {
        super("토큰의 권한이 적절하지 않습니다.", HttpStatus.FORBIDDEN);
    }

}
