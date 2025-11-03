package com.dongsoop.dongsoop.chat.dto;

import com.dongsoop.dongsoop.search.entity.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateChatRoomByAdminRequest(

        @NotNull
        @Positive()
        Long sourceUserId,

        @NotNull
        @Positive()
        Long targetUserId,

        @NotBlank
        String boardTitle,

        @NotNull
        BoardType boardType,

        @NotNull
        Long boardId
) {
}