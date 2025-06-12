package com.dongsoop.dongsoop.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReadStatusUpdateRequest {
    private LocalDateTime readUntilTime;
    private String lastReadMessageId;
}