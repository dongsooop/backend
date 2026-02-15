package com.dongsoop.dongsoop.blinddate.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과팅 시작 요청 참조: blind-date-reference/src/blinddate/dto/blinddate.available.dto.ts
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StartBlindDateRequest {

    @NotNull(message = "종료 시간은 필수입니다.")
    @Future(message = "종료 시간은 미래여야 합니다.")
    private LocalDateTime expiredDate;

    @NotNull(message = "세션당 최대 인원수는 필수입니다.")
    @Min(value = 2, message = "세션당 최대 인원수는 최소 2명 이상이어야 합니다.")
    private Integer maxSessionMemberCount;
}
