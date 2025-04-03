package com.dongsoop.dongsoop.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;

    public static ChatRoom create(String user1, String user2) {
        String roomId = UUID.randomUUID().toString();
        Set<String> participants = new HashSet<>();
        participants.add(user1);
        participants.add(user2);
        LocalDateTime now = LocalDateTime.now();

        return ChatRoom.builder()
                .roomId(roomId)
                .participants(participants)
                .createdAt(now)
                .lastActivityAt(now)
                .build();
    }

    public static ChatRoom createWithParticipants(Set<String> participants) {
        String roomId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        return ChatRoom.builder()
                .roomId(roomId)
                .participants(new HashSet<>(participants))
                .createdAt(now)
                .lastActivityAt(now)
                .build();
    }

    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
}