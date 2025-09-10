package com.dongsoop.dongsoop.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateContactRoomRequest {
    private Long targetUserId;
    private Object boardType;
    private Long boardId;
    private String boardTitle;
}
