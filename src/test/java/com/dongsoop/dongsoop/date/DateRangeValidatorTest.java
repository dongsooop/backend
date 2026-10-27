package com.dongsoop.dongsoop.date;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DateRangeValidatorTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void 날짜시간_범위는_public_접근자로_검증한다() {
        LocalDateTime startAt = LocalDateTime.of(2026, 10, 11, 10, 0);

        assertThat(validator.validate(new DateTimeRange(startAt, startAt.plusDays(1))))
                .isEmpty();
        assertThat(validator.validate(new DateTimeRange(startAt, startAt.plusHours(23))))
                .isNotEmpty();
        assertThat(validator.validate(new DateTimeRange(startAt, startAt.plusDays(29))))
                .isNotEmpty();
    }

    @Test
    void 사용자_지정_접근자_이름으로_순서를_검증한다() {
        LocalTime from = LocalTime.of(10, 0);

        assertThat(validator.validate(new TimeRange(from, from.plusHours(1))))
                .isEmpty();
        assertThat(validator.validate(new TimeRange(from, from.minusHours(1))))
                .isNotEmpty();
    }

    @Test
    void 시작이나_종료가_null이면_NotNull_검증에_위임한다() {
        assertThat(validator.validate(new DateTimeRange(null, null)))
                .isEmpty();
    }

    @MinDuration
    @MaxDuration
    @EndAtAfterStartAt
    public record DateTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
    }

    @EndAtAfterStartAt(start = "from", end = "to")
    public record TimeRange(LocalTime from, LocalTime to) {
    }
}
