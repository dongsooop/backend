package com.dongsoop.dongsoop.chat.dto;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ReadStatusUpdateRequest {
  private LocalDateTime readUntilTime;
  private String lastReadMessageId;
}
