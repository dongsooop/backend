package com.dongsoop.dongsoop.date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

public class EndAtAfterStartAtValidator implements ConstraintValidator<EndAtAfterStartAt, Object> {

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        try {
            LocalDateTime startAt = (LocalDateTime) object.getClass()
                    .getMethod("getStartAt")
                    .invoke(object);
            LocalDateTime endAt = (LocalDateTime) object.getClass()
                    .getMethod("getEndAt")
                    .invoke(object);

            if (startAt == null || endAt == null) {
                return true; // null 검증은 @NotNull 에서 처리
            }

            return endAt.isAfter(startAt);
        } catch (Exception e) {
            return false;
        }
    }
}
