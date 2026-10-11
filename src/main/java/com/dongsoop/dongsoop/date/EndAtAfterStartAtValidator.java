package com.dongsoop.dongsoop.date;

import com.dongsoop.dongsoop.date.exception.TimeTypeMismatchException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EndAtAfterStartAtValidator implements ConstraintValidator<EndAtAfterStartAt, Object> {

    private String startAccessorName;

    private String endAccessorName;

    @Override
    public void initialize(EndAtAfterStartAt constraintAnnotation) {
        this.startAccessorName = constraintAnnotation.start();
        this.endAccessorName = constraintAnnotation.end();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        try {
            Object startAtObject = object.getClass()
                    .getMethod(startAccessorName)
                    .invoke(object);
            Object endAtObject = object.getClass()
                    .getMethod(endAccessorName)
                    .invoke(object);
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
