package com.dongsoop.dongsoop.calendar.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MemberScheduleNotFoundException extends CustomException {

    public MemberScheduleNotFoundException(Long memberScheduleId) {
        super("회원 일정을 찾을 수 없습니다: " + memberScheduleId, HttpStatus.NOT_FOUND);
    }
}
