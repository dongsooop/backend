package com.dongsoop.dongsoop.blinddate.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BlindDateStorageTest 단위 테스트")
class BlindDateStorageTest {

    private BlindDateStorage storage;

    @BeforeEach
    void setUp() {
        storage = new BlindDateStorageImpl();
    }

    @Nested
    @DisplayName("start 메서드")
    class StartTest {

        @Test
        @DisplayName("과팅 시작 - available true")
        void start_SetsAvailableTrue() {
            // given
            LocalDateTime expiredDate = LocalDateTime.now().plusHours(1);

            // when
            storage.start(5, expiredDate);

            // then
            assertThat(storage.isAvailable()).isTrue();
            assertThat(storage.getMaxSessionMemberCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("과팅 시작 - 인원수와 종료시간 저장")
        void start_SavesParameters() {
            // given
            LocalDateTime expiredDate = LocalDateTime.of(2026, 10, 2, 15, 0);
            int maxCount = 10;

            // when
            storage.start(maxCount, expiredDate);

            // then
            assertThat(storage.getMaxSessionMemberCount()).isEqualTo(maxCount);
        }
    }

    @Nested
    @DisplayName("close 메서드")
    class CloseTest {

        @Test
        @DisplayName("과팅 종료 - available false")
        void close_SetsAvailableFalse() {
            // given
            storage.start(5, LocalDateTime.now().plusHours(1));
            assertThat(storage.isAvailable()).isTrue();

            // when
            storage.close();

            // then
            assertThat(storage.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Pointer 관리")
    class PointerTest {

        @Test
        @DisplayName("초기 상태 - Pointer null")
        void initial_PointerIsNull() {
            // when & then
            assertThat(storage.getPointer()).isNull();
        }

        @Test
        @DisplayName("setPointer - Pointer 설정")
        void setPointer_UpdatesPointer() {
            // given
            String sessionId = "session-123";

            // when
            storage.setPointer(sessionId);

            // then
            assertThat(storage.getPointer()).isEqualTo(sessionId);
        }


        @Test
        @DisplayName("여러 번 setPointer - 마지막 값으로 업데이트")
        void setPointerMultipleTimes_UpdatesToLatest() {
            // when
            storage.setPointer("session-1");
            storage.setPointer("session-2");
            storage.setPointer("session-3");

            // then
            assertThat(storage.getPointer()).isEqualTo("session-3");
        }
    }
}
