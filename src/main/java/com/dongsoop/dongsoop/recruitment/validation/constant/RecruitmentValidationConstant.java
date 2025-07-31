package com.dongsoop.dongsoop.recruitment.validation.constant;

import java.util.regex.Pattern;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class RecruitmentValidationConstant {

    public static final int TAG_MAX_LENGTH = 100;

    private static final String AVAILABLE_TAG_TEXT = "[a-zA-Z0-9ㄱ-ㅎ가-힣]";
    private static final String TAG_LENGTH_REGEX = "{1," + TAG_MAX_LENGTH + "}";
    private static final String START_TAG_REGEX = AVAILABLE_TAG_TEXT + TAG_LENGTH_REGEX;
    private static final String NEXT_TAG_PATTERN =
            "(," + AVAILABLE_TAG_TEXT + TAG_LENGTH_REGEX + ")*";
    public static final Pattern TAG_PATTERN = Pattern.compile(
            "^" + START_TAG_REGEX + NEXT_TAG_PATTERN + "$");
}
