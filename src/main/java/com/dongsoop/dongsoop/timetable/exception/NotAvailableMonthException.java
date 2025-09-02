package com.dongsoop.dongsoop.timetable.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotAvailableMonthException extends CustomException {

    public NotAvailableMonthException(int month) {
        super("해당 월의 시간표는 제공하지 않습니다: " + month, HttpStatus.BAD_REQUEST);
    }
}
