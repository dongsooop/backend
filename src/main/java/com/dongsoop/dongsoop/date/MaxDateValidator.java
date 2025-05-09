package com.dongsoop.dongsoop.date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

public class MaxDateValidator implements ConstraintValidator<MaxDate, LocalDateTime> {

    private int year;

    private int month;

    private int day;

    private int hour;

    private int minute;

    @Override
    public void initialize(MaxDate constraintAnnotation) {
        this.year = constraintAnnotation.year();
        this.month = constraintAnnotation.month();
        this.day = constraintAnnotation.day();
        this.hour = constraintAnnotation.hour();
        this.minute = constraintAnnotation.minute();
    }

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now()
                .plusMinutes(this.minute)
                .plusHours(this.hour)
                .plusDays(this.day)
                .plusMonths(this.month)
                .plusYears(this.year);

        return value.isBefore(now) || value.isEqual(now);
    }
}
