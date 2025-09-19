package com.dongsoop.dongsoop.chat.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;

public class ManagerLeaveRestrictedException extends CustomException {

    public ManagerLeaveRestrictedException(LocalDateTime recruitmentEndAt) {
        super(String.format("모집 기간 중에는 매니저가 채팅방을 나갈 수 없습니다. 모집 종료일: %s",
                        recruitmentEndAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))),
                HttpStatus.BAD_REQUEST);
    }
}