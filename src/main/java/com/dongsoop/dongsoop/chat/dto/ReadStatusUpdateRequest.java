package com.dongsoop.dongsoop.chat.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ReadStatusUpdateRequest {
    private LocalDateTime readUntilTime;
    private String lastReadMessageId;
}