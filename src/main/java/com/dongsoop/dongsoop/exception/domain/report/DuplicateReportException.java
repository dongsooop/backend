package com.dongsoop.dongsoop.exception.domain.report;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class DuplicateReportException extends CustomException {

    public DuplicateReportException() {
        super("이미 신고한 대상입니다.", HttpStatus.CONFLICT);
    }
}