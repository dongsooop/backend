package com.dongsoop.dongsoop.recruitment.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Constraint(validatedBy = {BoardTagValidator.class})
@ReportAsSingleViolation
public @interface BoardTag {

    String message() default "태그는 한글, 영문, 숫자만 포함할 수 있으며 쉼표(,)를 포함한 최대 100자입니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
