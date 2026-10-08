package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotReportableMessageException extends CustomException {

    public NotReportableMessageException() {
        super("시스템 메시지는 신고할 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
}
