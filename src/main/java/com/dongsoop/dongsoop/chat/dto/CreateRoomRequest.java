package com.dongsoop.dongsoop.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    @NotNull(message = "대상 사용자 ID는 필수입니다")
    private Long targetUserId;
    private String title;
}
