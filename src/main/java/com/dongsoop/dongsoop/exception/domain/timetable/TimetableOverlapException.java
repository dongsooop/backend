package com.dongsoop.dongsoop.exception.domain.timetable;

import com.dongsoop.dongsoop.exception.CustomException;
import java.time.LocalTime;
import org.springframework.http.HttpStatus;

public class TimetableOverlapException extends CustomException {

    public TimetableOverlapException(LocalTime startAt, LocalTime endAt, LocalTime overlapStartAt,
                                     LocalTime overlapEndAt) {
        super("겹치는 시간표가 존재합니다.\n"
                        + "[요청된 시간] 시작 시간: " + startAt + ", 종료 시간: " + endAt + "\n"
                        + "[저장된 시간] 시작 시간: " + overlapStartAt + ", 종료 시간: " + overlapEndAt,
                HttpStatus.BAD_REQUEST);
    }
}
