package com.dongsoop.dongsoop.date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EndAtAfterStartAtValidator implements ConstraintValidator<EndAtAfterStartAt, Object> {

    private String startFieldName;

    private String endFieldName;

    @Override
    public void initialize(EndAtAfterStartAt constraintAnnotation) {
        this.startFieldName = constraintAnnotation.start();
        this.endFieldName = constraintAnnotation.end();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        try {
            Field startField = object.getClass().getDeclaredField(startFieldName);
            Field endField = object.getClass().getDeclaredField(endFieldName);

            startField.setAccessible(true);
            endField.setAccessible(true);

            LocalDateTime startAt = (LocalDateTime) startField.get(object);
            LocalDateTime endAt = (LocalDateTime) endField.get(object);
            if (startAt == null || endAt == null) {
                return true; // null 검증은 @NotNull 에서 처리
            }

            return endAt.isAfter(startAt);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}
