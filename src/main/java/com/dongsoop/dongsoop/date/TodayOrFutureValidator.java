package com.dongsoop.dongsoop.date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TodayOrFutureValidator implements ConstraintValidator<TodayOrFuture, LocalDateTime> {

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate today = LocalDate.now();
        return !value.toLocalDate()
                .isBefore(today);
    }
}
