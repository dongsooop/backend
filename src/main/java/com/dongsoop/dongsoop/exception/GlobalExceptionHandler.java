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
        return createExceptionResponse(exception, exception.getHttpStatus());
    }

    // 인증 실패 (401 Unauthorized)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException exception) {
        return createExceptionResponse(exception, HttpStatus.UNAUTHORIZED);
    }

    // 권한 없음 (403 Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException exception) {
        return createExceptionResponse(exception, HttpStatus.FORBIDDEN);
    }

    // 정의되지 않은 예외처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception exception) {
        return createInternalServerErrorResponse(exception);
    }

    private ResponseEntity<ProblemDetail> createExceptionResponse(Exception exception, HttpStatus httpStatus) {
        ErrorResponse error = ErrorResponse.create(exception, httpStatus, exception.getMessage());
        ProblemDetail problemDetail = error.getBody();
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(httpStatus)
                .body(problemDetail);
    }

    private ResponseEntity<ProblemDetail> createInternalServerErrorResponse(Exception exception) {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorResponse error = ErrorResponse.create(exception, httpStatus, "알 수 없는 서버 오류가 발생했습니다.");
        ProblemDetail problemDetail = error.getBody();
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(httpStatus)
                .body(problemDetail);
    }
}
