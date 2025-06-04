package com.dongsoop.dongsoop.exception.domain.meal;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MealCrawlingException extends CustomException {

    public MealCrawlingException(String url) {
        super("식단 크롤링에 실패했습니다: " + url, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public MealCrawlingException(String url, Throwable cause) {
        super("식단 크롤링에 실패했습니다: " + url, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}