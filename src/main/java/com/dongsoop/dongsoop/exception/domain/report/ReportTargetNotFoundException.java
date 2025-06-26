package com.dongsoop.dongsoop.exception.domain.report;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ReportTargetNotFoundException extends CustomException {

    public ReportTargetNotFoundException(String targetType, Long targetId) {
        super("신고 대상을 찾을 수 없습니다. 타입: " + targetType + ", ID: " + targetId, HttpStatus.NOT_FOUND);
    }
}