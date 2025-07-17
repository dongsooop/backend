package com.dongsoop.dongsoop.recruitment.validation.annotation;

import com.dongsoop.dongsoop.recruitment.validation.constant.RecruitmentValidationConstant;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class BoardTagValidator implements ConstraintValidator<BoardTag, String> {

    @Override
    public boolean isValid(String tags, ConstraintValidatorContext constraintValidatorContext) {
        if (tags == null) {
            return false;
        }

        return !StringUtils.hasText(tags) || (
                tags.length() <= RecruitmentValidationConstant.TAG_MAX_LENGTH
                        && RecruitmentValidationConstant.TAG_PATTERN.matcher(tags).matches());
    }
}
