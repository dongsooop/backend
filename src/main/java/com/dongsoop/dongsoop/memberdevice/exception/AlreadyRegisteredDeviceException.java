package com.dongsoop.dongsoop.memberdevice.exception;

public class AlreadyRegisteredDeviceException extends RuntimeException {

    public AlreadyRegisteredDeviceException() {
        super("이미 등록된 디바이스입니다.");
    }
}
