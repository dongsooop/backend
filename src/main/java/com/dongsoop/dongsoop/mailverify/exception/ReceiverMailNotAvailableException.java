package com.dongsoop.dongsoop.mailverify.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ReceiverMailNotAvailableException extends CustomException {

    public ReceiverMailNotAvailableException(Throwable cause) {
        super("수신자 메일이 유효하지 않습니다.", HttpStatus.BAD_REQUEST, cause);
    }
}
