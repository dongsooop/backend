package com.dongsoop.dongsoop.meal.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;

public class MealNotFoundException extends CustomException {

    public MealNotFoundException(LocalDate date) {
        super("해당 날짜의 식단 정보를 찾을 수 없습니다: " + date, HttpStatus.NOT_FOUND);
    }

    public MealNotFoundException(LocalDate startDate, LocalDate endDate) {
        super("해당 기간의 식단 정보를 찾을 수 없습니다: " + startDate + " ~ " + endDate, HttpStatus.NOT_FOUND);
    }
}
