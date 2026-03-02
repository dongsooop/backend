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

    // 1:1 채팅방은 제목 없이 생성 가능 — 문의/거래 등 특수 채팅방은 서버에서 제목을 자동 생성함
    private String title;
}
