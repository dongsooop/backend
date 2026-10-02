package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class SanctionEndDateRequiredException extends CustomException {

    public SanctionEndDateRequiredException() {
        super("일시정지는 종료일을 입력해야 합니다.", HttpStatus.BAD_REQUEST);
    }
}
