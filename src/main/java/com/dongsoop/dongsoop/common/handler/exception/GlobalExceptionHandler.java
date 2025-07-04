package com.dongsoop.dongsoop.common.handler.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ProblemDetail> handleCustomException(CustomException exception) {
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

    // Validation 예외처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(MethodArgumentNotValidException exception) {
        return createExceptionResponse("요청 데이터의 포맷이 올바르지 않습니다.", exception, HttpStatus.BAD_REQUEST);
    }

    // 정의되지 않은 예외처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(Exception exception) {
        return createExceptionResponse("알 수 없는 서버 오류가 발생했습니다.", exception, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ProblemDetail> createExceptionResponse(Exception exception, HttpStatus httpStatus) {
        return createExceptionResponse(exception.getMessage(), exception, httpStatus);
    }

    private ResponseEntity<ProblemDetail> createExceptionResponse(String message, Exception exception,
                                                                  HttpStatus httpStatus) {
        log.error(message, exception);
        ErrorResponse error = ErrorResponse.create(exception, httpStatus, message);
        ProblemDetail problemDetail = error.getBody();
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(httpStatus)
                .body(problemDetail);
    }
}
