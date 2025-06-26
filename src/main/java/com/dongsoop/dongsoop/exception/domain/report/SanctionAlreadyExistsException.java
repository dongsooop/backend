package com.dongsoop.dongsoop.exception.domain.report;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class SanctionAlreadyExistsException extends CustomException {
    public SanctionAlreadyExistsException(Long reportId) {
        super("해당 신고에 대한 제재가 이미 존재합니다. Id : " + reportId, HttpStatus.CONFLICT);
    }
}
