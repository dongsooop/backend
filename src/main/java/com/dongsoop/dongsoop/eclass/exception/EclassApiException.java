package com.dongsoop.dongsoop.eclass.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class EclassApiException extends CustomException {

    public EclassApiException(String function, String errorCode) {
        super("이클래스 API 호출에 실패했습니다. function: " + function + ", error: " + errorCode,
                HttpStatus.BAD_GATEWAY);
    }

    public EclassApiException(String function, Throwable cause) {
        super("이클래스 API 호출에 실패했습니다. function: " + function, HttpStatus.BAD_GATEWAY, cause);
    }
}
