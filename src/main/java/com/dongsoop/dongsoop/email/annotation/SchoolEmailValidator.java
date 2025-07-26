package com.dongsoop.dongsoop.email.annotation;

import io.micrometer.common.util.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class SchoolEmailValidator implements ConstraintValidator<SchoolEmail, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9]+@dongyang.ac.kr$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null 또는 내용이 비어있는지에 대한 검증은 이 어노테이션에서 수행하지 않음
        // @NotBlank 어노테이션으로 처리 (역할분리)
        if (StringUtils.isBlank(value)) {
            return true;
        }

        return EMAIL_PATTERN.matcher(value).matches();
    }
}
