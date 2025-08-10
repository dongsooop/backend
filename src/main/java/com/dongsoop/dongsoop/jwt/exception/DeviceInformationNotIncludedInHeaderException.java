package com.dongsoop.dongsoop.jwt.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class DeviceInformationNotIncludedInHeaderException extends CustomException {

    public DeviceInformationNotIncludedInHeaderException() {
        super("디바이스 정보가 헤더에 포함되어 있지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}
