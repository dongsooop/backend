package com.dongsoop.dongsoop.blinddate.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * BlindDateInfoRepository 단위 테스트
 */
@DisplayName("BlindDateInfoRepository 단위 테스트")
class BlindDateInfoRepositoryTest {

    private BlindDateInfoRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new BlindDateInfoRepositoryImpl();
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
            repository.start(5, expiredDate);

            // then
            assertThat(repository.isAvailable()).isTrue();
            assertThat(repository.getMaxSessionMemberCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("과팅 시작 - 인원수와 종료시간 저장")
        void start_SavesParameters() {
            // given
            LocalDateTime expiredDate = LocalDateTime.of(2026, 10, 2, 15, 0);
            int maxCount = 10;

            // when
            repository.start(maxCount, expiredDate);

            // then
            assertThat(repository.getMaxSessionMemberCount()).isEqualTo(maxCount);
        }
    }

    @Nested
    @DisplayName("close 메서드")
    class CloseTest {

        @Test
        @DisplayName("과팅 종료 - available false")
        void close_SetsAvailableFalse() {
            // given
            repository.start(5, LocalDateTime.now().plusHours(1));
            assertThat(repository.isAvailable()).isTrue();

            // when
            repository.close();

            // then
            assertThat(repository.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Pointer 관리")
    class PointerTest {

        @Test
        @DisplayName("초기 상태 - Pointer null")
        void initial_PointerIsNull() {
            // when & then
            assertThat(repository.getPointer()).isNull();
        }

        @Test
        @DisplayName("setPointer - Pointer 설정")
        void setPointer_UpdatesPointer() {
            // given
            String sessionId = "session-123";

            // when
            repository.setPointer(sessionId);

            // then
            assertThat(repository.getPointer()).isEqualTo(sessionId);
        }


        @Test
        @DisplayName("여러 번 setPointer - 마지막 값으로 업데이트")
        void setPointerMultipleTimes_UpdatesToLatest() {
            // when
            repository.setPointer("session-1");
            repository.setPointer("session-2");
            repository.setPointer("session-3");

            // then
            assertThat(repository.getPointer()).isEqualTo("session-3");
        }
    }
}
