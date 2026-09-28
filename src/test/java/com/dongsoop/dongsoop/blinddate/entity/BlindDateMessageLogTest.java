package com.dongsoop.dongsoop.blinddate.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlindDateMessageLogTest {

    private static BlindDateMessage message(int i) {
        return new BlindDateMessage("m" + i, 1L, "내용" + i, LocalDateTime.now());
    }

    @Test
    @DisplayName("메시지를 ID로 찾고 직전 메시지를 최대 개수만큼 돌려준다")
    void findAndBefore() {
        BlindDateMessageLog log = new BlindDateMessageLog();
        for (int i = 1; i <= 15; i++) {
            log.add(message(i));
        }

        assertThat(log.find("m15")).map(BlindDateMessage::content).contains("내용15");
        assertThat(log.findBefore("m15", 10)).extracting(BlindDateMessage::messageId)
                .containsExactly("m5", "m6", "m7", "m8", "m9", "m10", "m11", "m12", "m13", "m14");
        assertThat(log.find("none")).isEmpty();
    }

    @Test
    @DisplayName("1,000개를 넘으면 가장 오래된 메시지부터 버린다")
    void dropsOldest() {
        BlindDateMessageLog log = new BlindDateMessageLog();
        for (int i = 1; i <= 1001; i++) {
            log.add(message(i));
        }

        assertThat(log.find("m1")).isEmpty();
        assertThat(log.find("m1001")).isPresent();
    }

    @Test
    @DisplayName("세션이 종료되면 보관한 메시지도 사라진다")
    void terminatedSessionForgetsMessages() {
        com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl storage =
                new com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl();
        String sessionId = storage.create().getSessionId();
        storage.recordMessage(sessionId, message(1));

        storage.terminate(sessionId);

        assertThat(storage.findMessage(sessionId, "m1")).isEmpty();
    }
}
