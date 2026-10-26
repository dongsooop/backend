package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class BlindDateMessageNotFoundException extends CustomException {

    public BlindDateMessageNotFoundException() {
        super("신고할 메시지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    }
}
