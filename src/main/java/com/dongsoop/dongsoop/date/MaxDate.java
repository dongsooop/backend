package com.dongsoop.dongsoop.date;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {MaxDateValidator.class})
public @interface MaxDate {

    int year() default 0;

    int month() default 0;

    int day() default 0;

    int hour() default 0;

    int minute() default 0;

    String message() default "날짜가 유효하지 않습니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
