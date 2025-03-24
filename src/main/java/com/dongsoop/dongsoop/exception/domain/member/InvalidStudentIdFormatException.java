package com.dongsoop.dongsoop.exception.domain.member;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidStudentIdFormatException extends CustomException {

    public InvalidStudentIdFormatException() {
        super("학번은 8자리 숫자여야 합니다.", HttpStatus.BAD_REQUEST);
    }

}