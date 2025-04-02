package com.dongsoop.dongsoop.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chat_rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomEntity {
    @Id
    private String roomId;

    @Column(nullable = false)
    private boolean isGroupChat;

    @ElementCollection
    @CollectionTable(name = "chat_room_participants",
            joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "participant_id")
    private Set<String> participants = new HashSet<>();

    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
}