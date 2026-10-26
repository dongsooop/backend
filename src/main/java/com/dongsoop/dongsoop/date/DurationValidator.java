package com.dongsoop.dongsoop.date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

public abstract class DurationValidator<A extends Annotation> implements ConstraintValidator<A, Object> {

    protected Long value;
    protected LocalDateTime startDateTime;
    protected LocalDateTime endDateTime;
    protected String startAccessorName;
    protected String endAccessorName;

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        try {
            if (setUp(object)) {
                return true;
            }

            return validateDuration();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean setUp(Object object)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        startDateTime = (LocalDateTime) object.getClass()
                .getMethod(this.startAccessorName)
                .invoke(object);
        endDateTime = (LocalDateTime) object.getClass()
                .getMethod(this.endAccessorName)
                .invoke(object);

        return this.startDateTime == null || this.endDateTime == null; // null 검증은 @NotNull 에서 처리
    }

    protected abstract boolean validateDuration();
}
