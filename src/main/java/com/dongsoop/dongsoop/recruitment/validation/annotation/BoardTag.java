package com.dongsoop.dongsoop.recruitment.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Pattern(regexp = "^[a-zA-Z0-9가-힣]*$")
@Size(max = 100)
@Constraint(validatedBy = {})
public @interface BoardTag {
}
