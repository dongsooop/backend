package com.dongsoop.dongsoop.report.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;

public record ChatMessageSnapshots(List<ChatMessageSnapshot> messages) {

    public ChatMessageSnapshots {
        messages = List.copyOf(messages);
    }

    @JsonValue
    @Override
    public List<ChatMessageSnapshot> messages() {
        return messages;
    }
}
