package com.dongsoop.dongsoop.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InviteUserRequest(
        @NotNull(message = "초대할 사용자 ID는 필수입니다")
        @Positive(message = "사용자 ID는 양수여야 합니다")
        Long targetUserId
) {}