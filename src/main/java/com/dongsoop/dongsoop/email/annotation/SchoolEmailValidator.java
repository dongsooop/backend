package com.dongsoop.dongsoop.email.annotation;

import io.micrometer.common.util.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SchoolEmailValidator implements ConstraintValidator<SchoolEmail, String> {

    private static final String EMAIL_REGEX = "^[a-zA-Z0-9]+@dongyang.ac.kr$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) { // 여기서 검증하지 않음
            return true;
        }

        return EMAIL_REGEX.matches(value);
    }
}
