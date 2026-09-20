package com.dongsoop.dongsoop.date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class MaxDateValidator implements ConstraintValidator<MaxDate, LocalDateTime> {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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

        LocalDateTime now = LocalDateTime.now(KST)
                .plusMinutes(this.minute)
                .plusHours(this.hour)
                .plusDays(this.day)
                .plusMonths(this.month)
                .plusYears(this.year);

        return value.isBefore(now) || value.isEqual(now);
    }
}
