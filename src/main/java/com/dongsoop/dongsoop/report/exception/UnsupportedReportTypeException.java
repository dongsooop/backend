package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UnsupportedReportTypeException extends CustomException {

    public UnsupportedReportTypeException() {
        super("채팅 메시지 신고는 전용 API를 사용해야 합니다.", HttpStatus.BAD_REQUEST);
    }
}
