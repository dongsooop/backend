package com.dongsoop.dongsoop.chat.dto;

import com.dongsoop.dongsoop.search.entity.BoardType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateContactRoomRequest {
    private Long targetUserId;
    private BoardType boardType;
    private Long boardId;
    private String boardTitle;
}
