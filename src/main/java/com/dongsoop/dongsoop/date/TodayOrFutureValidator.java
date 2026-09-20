package com.dongsoop.dongsoop.date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TodayOrFutureValidator implements ConstraintValidator<TodayOrFuture, LocalDateTime> {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        LocalDate today = LocalDate.now(KST);
        LocalDate valueDate = value.toLocalDate();

        return today.equals(valueDate) || today.isBefore(valueDate);
    }
}
