package com.dongsoop.dongsoop.chat.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UnauthorizedManagerActionException extends CustomException {
    public UnauthorizedManagerActionException() {
        super("매니저만 이 작업을 수행할 수 있습니다.", HttpStatus.FORBIDDEN);
    }
}
