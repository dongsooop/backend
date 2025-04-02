package com.dongsoop.dongsoop.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRoomRequest {
    private Set<String> participants;
}
