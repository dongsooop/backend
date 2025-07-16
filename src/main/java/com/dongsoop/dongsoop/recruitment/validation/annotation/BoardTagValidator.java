package com.dongsoop.dongsoop.recruitment.validation.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public class BoardTagValidator implements ConstraintValidator<BoardTag, String> {

    private static final int MAX_LENGTH = 100;
    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9가-힣,]*[a-zA-Z0-9가-힣]+$");

    @Override
    public boolean isValid(String tags, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.hasText(tags) && tags.length() <= MAX_LENGTH && TAG_PATTERN.matcher(tags).matches();
    }
}
