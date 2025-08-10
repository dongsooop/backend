package com.dongsoop.dongsoop.memberdevice.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UnregisteredDeviceException extends CustomException {

    public UnregisteredDeviceException() {
        super("등록되지 않은 기기입니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
