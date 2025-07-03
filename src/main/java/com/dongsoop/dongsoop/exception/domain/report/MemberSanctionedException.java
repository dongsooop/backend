package com.dongsoop.dongsoop.exception.domain.report;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MemberSanctionedException extends CustomException {

    public MemberSanctionedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}