package com.dongsoop.dongsoop.notification.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ResponseSizeUnmatchedToTokenSizeException extends CustomException {

    public ResponseSizeUnmatchedToTokenSizeException(int responseSize, int tokenSize) {
        super(String.format("알림 응답의 크기가 토큰 목록의 크기와 일치하지 않습니다.%n응답 수: %d, 토큰 수: %d", responseSize, tokenSize),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
