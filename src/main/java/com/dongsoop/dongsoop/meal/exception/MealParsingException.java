package com.dongsoop.dongsoop.meal.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MealParsingException extends CustomException {

    public MealParsingException(String url) {
        super("식단 파싱 중 오류가 발생했습니다: " + url, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public MealParsingException(String url, Throwable cause) {
        super("식단 파싱 중 오류가 발생했습니다: " + url, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
