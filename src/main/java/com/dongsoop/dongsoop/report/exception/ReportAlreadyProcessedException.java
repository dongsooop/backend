package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ReportAlreadyProcessedException extends CustomException {

    public ReportAlreadyProcessedException(Long reportId) {
        super("이미 처리된 신고입니다. ID : " + reportId, HttpStatus.CONFLICT);
    }
}
