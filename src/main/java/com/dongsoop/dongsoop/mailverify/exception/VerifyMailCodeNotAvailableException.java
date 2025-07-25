package com.dongsoop.dongsoop.mailverify.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class VerifyMailCodeNotAvailableException extends CustomException {

    public VerifyMailCodeNotAvailableException() {
        super("인증 코드가 유효하지 않습니다. 다시 시도해주세요.", HttpStatus.BAD_REQUEST);
    }
}
