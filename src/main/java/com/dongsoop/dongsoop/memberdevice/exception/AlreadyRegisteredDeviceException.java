package com.dongsoop.dongsoop.memberdevice.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class AlreadyRegisteredDeviceException extends CustomException {

    public AlreadyRegisteredDeviceException() {
        super("이미 등록된 디바이스입니다.", HttpStatus.CONFLICT);
    }
}
