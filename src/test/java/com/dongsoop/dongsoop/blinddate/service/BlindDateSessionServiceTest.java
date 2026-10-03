package com.dongsoop.dongsoop.blinddate.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorageImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

@DisplayName("과팅 정원 판정 단위 결과")
class BlindDateSessionServiceTest {
    @ParameterizedTest(name = "정원 2명, 참가 {0}명 -> full={1}")
    @CsvSource({"0,false", "1,false", "2,true", "3,true"})
    void fullnessThreshold(int count, boolean expected) {
        var participants = new BlindDateParticipantStorageImpl();
        var operation = new BlindDateStorageImpl();
        operation.start(2, LocalDateTime.now().plusHours(1));
        for (long id = 1; id <= count; id++) participants.addParticipant("a", id, "s" + id);
        var service = new BlindDateSessionServiceImpl(participants, operation);
        assertThat(service.isSessionFull("a")).isEqualTo(expected);
        assertThat(service.isSessionFull("other")).isFalse();
    }

    @Test
    @DisplayName("운영 정원이 없으면 세션은 가득 차지 않음")
    void noConfiguredCapacity() {
        var service =
                new BlindDateSessionServiceImpl(
                        new BlindDateParticipantStorageImpl(), new BlindDateStorageImpl());
        assertThat(service.isSessionFull("a")).isFalse();
    }
}
