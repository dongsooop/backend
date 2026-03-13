package com.dongsoop.dongsoop.appcheck.web.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class WebAppCheckTokenIssuanceException extends CustomException {

    public WebAppCheckTokenIssuanceException(Exception cause) {
        super("앱 체크 토큰 발급에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
