package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MemberSanctionedException extends CustomException {

    public MemberSanctionedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
