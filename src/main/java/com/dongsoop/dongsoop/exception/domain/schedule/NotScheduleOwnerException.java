package com.dongsoop.dongsoop.exception.domain.schedule;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotScheduleOwnerException extends CustomException {

    public NotScheduleOwnerException(Long requestMemberId, Long ownerMemberId, Long memberScheduleId) {
        super("요청한 회원의 일정이 아닙니다. 요청 회원 ID: " + requestMemberId + ", 소유 회원 ID: " + ownerMemberId + ", 회원 일정 ID: "
                + memberScheduleId, HttpStatus.NOT_FOUND);
    }
}
