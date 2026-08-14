package com.dongsoop.dongsoop.s3.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidFileException extends CustomException {

    public InvalidFileException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
