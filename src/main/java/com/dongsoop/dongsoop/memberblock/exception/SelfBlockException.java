package com.dongsoop.dongsoop.memberblock.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class SelfBlockException extends CustomException {

    public SelfBlockException() {
        super("자기 자신은 차단할 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
}
