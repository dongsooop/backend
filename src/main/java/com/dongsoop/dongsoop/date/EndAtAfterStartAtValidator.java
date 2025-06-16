package com.dongsoop.dongsoop.date;

import com.dongsoop.dongsoop.exception.domain.date.TimeTypeMismatchException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

            Object startAtObject = startField.get(object);
            Object endAtObject = endField.get(object);
            if (startAtObject == null || endAtObject == null) {
                return true; // null 검증은 @NotNull 에서 처리
            }

            return compare(startAtObject, endAtObject);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private boolean compare(Object startAtObject, Object endAtObject) {
        if (endAtObject instanceof LocalDateTime endAt && startAtObject instanceof LocalDateTime startAt) {
            return endAt.isAfter(startAt);
        }

        if (endAtObject instanceof LocalTime endAt && startAtObject instanceof LocalTime startAt) {
            return endAt.isAfter(startAt);
        }

        throw new TimeTypeMismatchException();
    }
}
