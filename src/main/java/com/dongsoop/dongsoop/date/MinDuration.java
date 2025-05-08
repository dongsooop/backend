package com.dongsoop.dongsoop.date;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {MinDurationValidator.class})
public @interface MinDuration {

    long value() default 86400L;

    String start() default "startAt";

    String end() default "endAt";

    String message() default "시작일이 종료일보다 늦을 수 없습니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
