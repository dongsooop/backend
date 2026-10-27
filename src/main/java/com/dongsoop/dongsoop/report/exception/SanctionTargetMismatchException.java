package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class SanctionTargetMismatchException extends CustomException {

    public SanctionTargetMismatchException() {
        super("제재 대상이 신고된 메시지의 작성자와 다릅니다.", HttpStatus.BAD_REQUEST);
    }
}
