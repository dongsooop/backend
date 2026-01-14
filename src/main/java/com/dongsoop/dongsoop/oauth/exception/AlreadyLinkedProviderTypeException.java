package com.dongsoop.dongsoop.oauth.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class AlreadyLinkedProviderTypeException extends CustomException {

    public AlreadyLinkedProviderTypeException() {
        super("이미 해당 공급자로 가입된 계정이 존재합니다.", HttpStatus.CONFLICT);
    }
}
