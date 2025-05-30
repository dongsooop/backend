package com.dongsoop.dongsoop.exception.domain.schedule;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotScheduleOwnerException extends CustomException {

    public NotScheduleOwnerException(Long memberId, Long memberScheduleId) {
        super("요청한 회원의 일정이 아닙니다. 회원 ID:" + memberId + ", 회원 일정 ID: " + memberScheduleId, HttpStatus.NOT_FOUND);
    }
}
