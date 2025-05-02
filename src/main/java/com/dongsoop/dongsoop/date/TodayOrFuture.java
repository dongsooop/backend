package com.dongsoop.dongsoop.date;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {TodayOrFutureValidator.class})
public @interface TodayOrFuture {

    String message() default "오늘보다 과거일 수 없습니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
