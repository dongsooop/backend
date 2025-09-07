package com.dongsoop.dongsoop.home.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class HomeAsyncException extends CustomException {

    public HomeAsyncException(Throwable cause) {
        super("홈 데이터 수집 중 문제가 발생했습니다: \n", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
