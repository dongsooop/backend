package com.dongsoop.dongsoop.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(CustomException exception) {
        ErrorResponse error = ErrorResponse.create(exception, exception.getHttpStatus(), exception.getMessage());
        ProblemDetail problemDetail = error.getBody();

        // 예외 발생 시간
        problemDetail.setProperty("timestamp", exception.getTimestamp());

        return ResponseEntity.status(exception.getHttpStatus())
                .body(error.getBody());
    }
}
