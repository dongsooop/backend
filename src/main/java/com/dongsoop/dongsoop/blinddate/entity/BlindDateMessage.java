package com.dongsoop.dongsoop.blinddate.entity;

import java.time.LocalDateTime;

public record BlindDateMessage(String messageId, Long senderId, String content, LocalDateTime sentAt) {
}
