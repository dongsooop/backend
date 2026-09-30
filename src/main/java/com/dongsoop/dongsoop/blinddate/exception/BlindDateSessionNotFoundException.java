package com.dongsoop.dongsoop.blinddate.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class BlindDateSessionNotFoundException extends CustomException {

    public BlindDateSessionNotFoundException() {
        super("과팅 세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    }
}
