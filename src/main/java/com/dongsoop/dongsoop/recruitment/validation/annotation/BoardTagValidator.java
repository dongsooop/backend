package com.dongsoop.dongsoop.recruitment.validation.annotation;

import com.dongsoop.dongsoop.recruitment.validation.constant.RecruitmentValidationConstant;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BoardTagValidator implements ConstraintValidator<BoardTag, String> {

    @Override
    public boolean isValid(String tags, ConstraintValidatorContext constraintValidatorContext) {
        if (tags == null) {
            return false;
        }

        return tags.trim().isEmpty() || (
                tags.length() <= RecruitmentValidationConstant.TAG_MAX_LENGTH
                        && RecruitmentValidationConstant.TAG_PATTERN.matcher(tags).matches());
    }
}
