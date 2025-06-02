package com.dongsoop.dongsoop.exception;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
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

    // 인증 실패 (401 Unauthorized)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(
            org.springframework.security.core.AuthenticationException exception) {
        ErrorResponse error = ErrorResponse.create(exception, HttpStatus.UNAUTHORIZED, exception.getMessage());
        ProblemDetail problemDetail = error.getBody();
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(problemDetail);
    }

    // 권한 없음 (403 Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException exception) {
        ErrorResponse error = ErrorResponse.create(exception, HttpStatus.FORBIDDEN, exception.getMessage());
        ProblemDetail problemDetail = error.getBody();
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(problemDetail);
    }
}
