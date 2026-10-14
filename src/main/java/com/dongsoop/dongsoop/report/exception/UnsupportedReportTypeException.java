package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UnsupportedReportTypeException extends CustomException {

    public UnsupportedReportTypeException() {
        super("이 API에서 지원하지 않는 신고 유형입니다.", HttpStatus.BAD_REQUEST);
    }
}
