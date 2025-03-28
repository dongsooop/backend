package com.dongsoop.dongsoop.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    private String roomId;
    private Set<String> participants;

    public static ChatRoom create(String user1, String user2) {
        String roomId = UUID.randomUUID().toString();
        Set<String> participants = new HashSet<>();
        participants.add(user1);
        participants.add(user2);

        return ChatRoom.builder()
                .roomId(roomId)
                .participants(participants)
                .build();
    }
}
