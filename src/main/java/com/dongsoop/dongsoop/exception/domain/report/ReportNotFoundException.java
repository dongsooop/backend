package com.dongsoop.dongsoop.exception.domain.report;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ReportNotFoundException extends CustomException {
    public ReportNotFoundException(Long reportId) {
        super("존재하지 않는 신고합니다. ID :" + reportId, HttpStatus.NOT_FOUND);
    }
}
