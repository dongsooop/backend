package com.dongsoop.dongsoop.blinddate.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.blinddate.exception.InvalidBlindDateChoiceException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BlindDateChoiceValidationTest {
    @ParameterizedTest
    @CsvSource({
        "1, 1, 400, INVALID_CHOICE",
        "99, 1, 403, CHOICE_FORBIDDEN",
        "3, 1, 403, CHOICE_FORBIDDEN",
        "1, 99, 404, CHOICE_TARGET_NOT_FOUND",
        "1, 3, 404, CHOICE_TARGET_NOT_FOUND"
    })
    void invalidChoiceDoesNotConsumeFirstChoice(
            Long chooser, Long target, int status, String code) {
        var storage = new BlindDateParticipantStorageImpl();
        storage.addParticipant("session", 1L, "one");
        storage.addParticipant("session", 2L, "two");
        storage.addParticipant("other", 3L, "three");

        storage.openChoices("session");

        assertThatThrownBy(() -> storage.recordChoice("session", chooser, target))
                .isInstanceOfSatisfying(
                        InvalidBlindDateChoiceException.class,
                        error -> {
                            assertThat(error.getStatus()).isEqualTo(status);
                            assertThat(error.getCode()).isEqualTo(code);
                        });
        assertThat(storage.getNotMatched("session")).containsExactlyInAnyOrder(1L, 2L);
        assertThat(storage.getNotMatched("other")).containsExactly(3L);

        assertThat(storage.recordChoice("session", 1L, 2L)).isTrue();
        assertThat(storage.recordChoice("session", 2L, 1L)).isTrue();
        assertThat(storage.getNotMatched("session")).isEmpty();
        assertThat(storage.getNotMatched("other")).containsExactly(3L);
    }
}
