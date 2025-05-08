package com.dongsoop.dongsoop.date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class MaxDurationValidator implements ConstraintValidator<MaxDuration, Object> {

    private Long value;

    private String startFieldName;

    private String endFieldName;

    @Override
    public void initialize(MaxDuration constraintAnnotation) {
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

            LocalDateTime maxTime = startAt.plusSeconds(this.value);
            return endAt.isBefore(maxTime) || endAt.equals(maxTime);
        } catch (Exception e) {
            return false;
        }
    }
}
