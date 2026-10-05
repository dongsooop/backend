package com.dongsoop.dongsoop.blinddate.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

@DisplayName("과팅 개최 요청 검증")
class BlindDateRequestValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(null, 2, "expiredDate"),
                Arguments.of(LocalDateTime.now().minusHours(1), 2, "expiredDate"),
                Arguments.of(LocalDateTime.now().plusHours(1), null, "maxSessionMemberCount"),
                Arguments.of(LocalDateTime.now().plusHours(1), 1, "maxSessionMemberCount"),
                Arguments.of(LocalDateTime.now().plusHours(1), 0, "maxSessionMemberCount"),
                Arguments.of(LocalDateTime.now().plusHours(1), -1, "maxSessionMemberCount"));
    }

    @Test
    @DisplayName("B01 미래 시간·최소 2명 유효")
    void minimumValidRequest() {
        assertThat(
                        validator.validate(
                                new StartBlindDateRequest(LocalDateTime.now().plusHours(1), 2)))
                .isEmpty();
    }

    @ParameterizedTest(name = "B02 {2} 거부: 시간={0}, 정원={1}")
    @MethodSource("invalidRequests")
    void rejectsInvalidFields(LocalDateTime time, Integer count, String field) {
        assertThat(validator.validate(new StartBlindDateRequest(time, count)))
                .extracting(v -> v.getPropertyPath().toString())
                .contains(field);
    }
}
