package com.dongsoop.dongsoop.calendar.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ScheduleAlreadySetByMemberException extends CustomException {

    public ScheduleAlreadySetByMemberException(Long requestMemberId, Long setMemberId) {
        super("해당 스케줄에 이미 회원이 설정되었습니다. 요청 회원 ID: " + requestMemberId + ", 설정된 회원 ID: " + setMemberId,
                HttpStatus.BAD_REQUEST);
    }
}
