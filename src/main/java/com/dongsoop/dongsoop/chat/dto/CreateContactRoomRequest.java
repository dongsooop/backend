package com.dongsoop.dongsoop.chat.dto;

import com.dongsoop.dongsoop.search.entity.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateContactRoomRequest {
    @NotNull(message = "대상 사용자 ID는 필수입니다")
    private Long targetUserId;
    @NotNull(message = "게시판 유형은 필수입니다")
    private BoardType boardType;
    @NotNull(message = "게시판 ID는 필수입니다")
    private Long boardId;
    @NotBlank(message = "게시판 제목은 필수입니다")
    private String boardTitle;
}
