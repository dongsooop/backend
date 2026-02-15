package com.dongsoop.dongsoop.blinddate.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.service.BlindDateService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * BlindDateController 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BlindDateController 단위 테스트")
class BlindDateControllerTest {

    @Mock
    private BlindDateService blindDateService;

    @InjectMocks
    private BlindDateController controller;

    @Nested
    @DisplayName("GET /blinddate - 과팅 운영 중 확인")
    class IsAvailableTest {

        @Test
        @DisplayName("과팅 운영 중 - true 반환")
        void whenAvailable_thenReturnTrue() {
            // given
            given(blindDateService.isAvailable()).willReturn(true);

            // when
            ResponseEntity<Boolean> response = controller.isAvailable();

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isTrue();
            verify(blindDateService).isAvailable();
        }

        @Test
        @DisplayName("과팅 종료 상태 - false 반환")
        void whenNotAvailable_thenReturnFalse() {
            // given
            given(blindDateService.isAvailable()).willReturn(false);

            // when
            ResponseEntity<Boolean> response = controller.isAvailable();

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isFalse();
            verify(blindDateService).isAvailable();
        }
    }

    @Nested
    @DisplayName("POST /blinddate - 과팅 시작")
    class StartBlindDateTest {

        @Test
        @DisplayName("과팅 시작 성공 - 201 CREATED")
        void startBlindDate_ReturnsCreated() {
            // given
            LocalDateTime expiredDate = LocalDateTime.now().plusHours(1);
            StartBlindDateRequest request = new StartBlindDateRequest(expiredDate, 5);
            given(blindDateService.isAvailable()).willReturn(false);

            // when
            ResponseEntity<Void> response = controller.startBlindDate(request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(blindDateService).startBlindDate(request);
        }

        @Test
        @DisplayName("이미 운영 중 - 409 CONFLICT (무시)")
        void whenAlreadyAvailable_thenReturnConflict() {
            // given
            LocalDateTime expiredDate = LocalDateTime.now().plusHours(1);
            StartBlindDateRequest request = new StartBlindDateRequest(expiredDate, 5);
            given(blindDateService.isAvailable()).willReturn(true);

            // when
            ResponseEntity<Void> response = controller.startBlindDate(request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            verify(blindDateService, times(0)).startBlindDate(any());
        }

        @Test
        @DisplayName("과팅 시작 - Service 메서드 호출 검증")
        void startBlindDate_CallsService() {
            // given
            LocalDateTime expiredDate = LocalDateTime.of(2026, 10, 2, 15, 0);
            StartBlindDateRequest request = new StartBlindDateRequest(expiredDate, 3);
            given(blindDateService.isAvailable()).willReturn(false);

            // when
            controller.startBlindDate(request);

            // then
            verify(blindDateService).isAvailable();
            verify(blindDateService).startBlindDate(request);
        }
    }
}
