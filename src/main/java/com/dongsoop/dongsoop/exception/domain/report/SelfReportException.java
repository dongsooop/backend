package com.dongsoop.dongsoop.exception.domain.report;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class SelfReportException extends CustomException {

    public SelfReportException() {
        super("자기 자신을 신고할 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
}