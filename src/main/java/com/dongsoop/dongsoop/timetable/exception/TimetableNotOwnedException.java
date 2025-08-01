package com.dongsoop.dongsoop.timetable.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TimetableNotOwnedException extends CustomException {

    public TimetableNotOwnedException(Long timetableId, Long memberId) {
        super("사용자의 시간표가 아닙니다. 시간표 id: " + timetableId + ", 사용자 ID: " + memberId,
                HttpStatus.BAD_REQUEST);
    }
}
