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

    @Column
    private Long managerId;

    @ElementCollection
    @CollectionTable(name = "chat_room_participants",
            joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "participant_id")
    private Set<Long> participants = new HashSet<>();

    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;

    public ChatRoom toChatRoom() {
        return ChatRoom.builder()
                .roomId(this.roomId)
                .participants(new HashSet<>(this.participants))
                .managerId(this.managerId)
                .isGroupChat(this.isGroupChat)
                .createdAt(this.createdAt)
                .lastActivityAt(this.lastActivityAt)
                .build();
    }
}