package com.dongsoop.dongsoop.oauth.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidKakaoTokenException extends CustomException {

    public InvalidKakaoTokenException() {
        super("유효하지 않은 카카오 토큰입니다.", HttpStatus.UNAUTHORIZED);
    }
}
