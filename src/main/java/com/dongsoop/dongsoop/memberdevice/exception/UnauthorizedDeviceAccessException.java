package com.dongsoop.dongsoop.memberdevice.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

/**
 * 본인 소유가 아닌 기기에 접근하려 할 때 발생하는 예외.
 *
 * <p>강제 로그아웃 등 기기 관련 작업에서 요청한 회원이 해당 기기의 소유자가 아닐 경우 던진다.
 * HTTP 403 Forbidden으로 응답된다.
 */
public class UnauthorizedDeviceAccessException extends CustomException {

    public UnauthorizedDeviceAccessException() {
        super("해당 디바이스에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
}
