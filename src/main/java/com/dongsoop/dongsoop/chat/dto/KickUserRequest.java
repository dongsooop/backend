package com.dongsoop.dongsoop.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KickUserRequest {
    @NotNull(message = "강퇴할 사용자 ID는 필수입니다")
    private Long userId;
}
