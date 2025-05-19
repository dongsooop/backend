package com.dongsoop.dongsoop.date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TodayOrFutureValidator implements ConstraintValidator<TodayOrFuture, LocalDateTime> {

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate valueDate = value.toLocalDate();

        return today.equals(valueDate) || today.isBefore(valueDate);
    }
}
