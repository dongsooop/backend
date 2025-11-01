package com.dongsoop.dongsoop.mailverify.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MissingMailVerifyException extends CustomException {

    public MissingMailVerifyException(Integer minute) {
        super(String.format("이메일 인증이 누락되었거나, 이메일 인증 후 가입 완료 시간(%d분)을 초과했습니다.\n다시 시도해주세요.", minute),
                HttpStatus.BAD_REQUEST);
    }
}
