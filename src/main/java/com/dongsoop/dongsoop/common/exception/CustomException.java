package com.dongsoop.dongsoop.common.exception;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final HttpStatus httpStatus;
    private final LocalDateTime timestamp = LocalDateTime.now(KST);

    public CustomException(String message, HttpStatus httpStatus) {
        super(message);

        this.httpStatus = httpStatus;
    }

    public CustomException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);

        this.httpStatus = httpStatus;
    }

}
