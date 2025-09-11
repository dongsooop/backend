package com.dongsoop.dongsoop.chat.dto;

import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateContactRoomRequest {
    private Long targetUserId;
    private RecruitmentType boardType;
    private Long boardId;
    private String boardTitle;
}
