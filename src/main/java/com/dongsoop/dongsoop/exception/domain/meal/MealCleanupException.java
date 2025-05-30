package com.dongsoop.dongsoop.exception.domain.meal;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MealCleanupException extends CustomException {

    public MealCleanupException() {
        super("식단 데이터 정리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public MealCleanupException(Throwable cause) {
        super("식단 데이터 정리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}