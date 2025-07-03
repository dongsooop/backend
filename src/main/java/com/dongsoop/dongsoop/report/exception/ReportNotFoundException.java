package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ReportNotFoundException extends CustomException {
    public ReportNotFoundException(Long reportId) {
        super("존재하지 않는 신고입니다. ID :" + reportId, HttpStatus.NOT_FOUND);
    }
}
