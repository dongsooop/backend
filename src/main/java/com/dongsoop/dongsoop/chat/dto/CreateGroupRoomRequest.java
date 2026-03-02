package com.dongsoop.dongsoop.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRoomRequest {
    @NotNull(message = "참여자 목록은 필수입니다")
    @Size(min = 1, message = "참여자는 최소 1명 이상이어야 합니다")
    private Set<@NotNull(message = "참여자 ID는 null일 수 없습니다") Long> participants;
    private String title;
}
