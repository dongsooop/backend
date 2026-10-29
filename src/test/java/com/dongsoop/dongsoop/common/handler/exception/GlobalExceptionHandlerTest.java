package com.dongsoop.dongsoop.common.handler.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.report.dto.SanctionStatusResponse;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.exception.MemberSanctionedException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("정지 회원 로그인 거절은 403 본문 sanction에 제재 유형·사유·설명·기간을 담는다")
  void memberSanctioned_IncludesSanctionInBody() {
    LocalDateTime start = LocalDateTime.of(2026, 10, 16, 10, 0);
    LocalDateTime end = start.plusDays(3);
    Sanction sanction =
        Sanction.builder()
            .sanctionType(SanctionType.TEMPORARY_BAN)
            .reason("욕설")
            .description("경고 3회 누적으로 인한 자동 3일 정지")
            .startDate(start)
            .endDate(end)
            .build();

    ResponseEntity<ProblemDetail> response =
        handler.handleMemberSanctionedException(new MemberSanctionedException(sanction));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody().getProperties().get("sanction"))
        .isEqualTo(
            new SanctionStatusResponse(
                true, "TEMPORARY_BAN", "욕설", start, end, "경고 3회 누적으로 인한 자동 3일 정지"));
  }
}
