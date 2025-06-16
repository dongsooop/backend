package com.dongsoop.dongsoop.exception.domain.date;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TimeTypeMismatchException extends CustomException {

    public TimeTypeMismatchException() {
        super("두 날짜 또는 시간 변수의 타입이 올바르지 않습니다", HttpStatus.BAD_REQUEST);
    }
}
