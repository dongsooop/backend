package com.dongsoop.dongsoop.timetable.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TimetableNotFoundException extends CustomException {

    public TimetableNotFoundException(Long timetableId) {
        super("존재하지 않는 시간표 ID입니다: " + timetableId, HttpStatus.BAD_REQUEST);
    }
}
