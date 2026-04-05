package com.dongsoop.dongsoop.memberdevice.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class WebDeviceLimitExceededException extends CustomException {

    public WebDeviceLimitExceededException() {
        super("WEB 디바이스는 최대 3개까지 등록 가능합니다.", HttpStatus.BAD_REQUEST);
    }
}