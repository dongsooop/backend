package com.dongsoop.dongsoop.memberblock.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class AlreadyBlockedByBlockerException extends CustomException {

    public AlreadyBlockedByBlockerException() {
        super("이미 차단한 유저입니다.", HttpStatus.BAD_REQUEST);
    }
}
