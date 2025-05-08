package com.dongsoop.dongsoop.date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class MinDurationValidator implements ConstraintValidator<MinDuration, Object> {

    private Long value;

    private String startFieldName;

    private String endFieldName;

    @Override
    public void initialize(MinDuration constraintAnnotation) {
        this.value = constraintAnnotation.value();
        this.startFieldName = constraintAnnotation.start();
        this.endFieldName = constraintAnnotation.end();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        try {
            Field startField = object.getClass()
                    .getDeclaredField(startFieldName);
            Field endField = object.getClass()
                    .getDeclaredField(endFieldName);

            startField.setAccessible(true);
            endField.setAccessible(true);

            LocalDateTime startAt = (LocalDateTime) startField.get(object);
            LocalDateTime endAt = (LocalDateTime) endField.get(object);

            if (startAt == null || endAt == null) {
                return true; // null 검증은 @NotNull 에서 처리
            }

            LocalDateTime minTime = endAt.minusSeconds(this.value);
            return startAt.isBefore(minTime) || startAt.equals(minTime);
        } catch (Exception e) {
            return false;
        }
    }
}
