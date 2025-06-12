package com.dongsoop.dongsoop.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncrementalSyncResponse {
    private String roomId;
    private List<ChatMessage> newMessages;
    private int unreadCount;
    private LocalDateTime lastSyncTime;

    public static IncrementalSyncResponse create(String roomId, List<ChatMessage> newMessages, int unreadCount) {
        return IncrementalSyncResponse.builder()
                .roomId(roomId)
                .newMessages(newMessages)
                .unreadCount(unreadCount)
                .lastSyncTime(LocalDateTime.now())
                .build();
    }
}