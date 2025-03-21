package com.dongsoop.dongsoop.exception;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {

    private HttpStatus httpStatus;
    private LocalDateTime timestamp = LocalDateTime.now();

    public CustomException(String message, HttpStatus httpStatus) {
        super(message);

        this.httpStatus = httpStatus;
    }

    public CustomException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);

        this.httpStatus = httpStatus;
    }

}
