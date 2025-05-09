package com.dongsoop.dongsoop.date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

public abstract class DurationValidator<A extends Annotation> implements ConstraintValidator<A, Object> {

    protected Long value;
    protected LocalDateTime startDateTime;
    protected LocalDateTime endDateTime;
    protected String startFieldName;
    protected String endFieldName;

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        try {
            setUp(object);

            return validateDuration();
        } catch (NoSuchFieldException e) {
            return false;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean setUp(Object object) throws NoSuchFieldException, IllegalAccessException {
        Field startField = object.getClass()
                .getDeclaredField(this.startFieldName);
        Field endField = object.getClass()
                .getDeclaredField(this.endFieldName);

        startField.setAccessible(true);
        endField.setAccessible(true);

        startDateTime = (LocalDateTime) startField.get(object);
        endDateTime = (LocalDateTime) endField.get(object);

        return this.startDateTime == null || this.endDateTime == null; // null 검증은 @NotNull 에서 처리
    }

    protected abstract boolean validateDuration();
}
