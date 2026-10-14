package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class BlindDateSessionEndedException extends CustomException {

    public BlindDateSessionEndedException() {
        super("종료된 과팅의 메시지는 신고할 수 없습니다.", HttpStatus.GONE);
    }
}
