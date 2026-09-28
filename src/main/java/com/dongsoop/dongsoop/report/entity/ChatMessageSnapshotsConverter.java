package com.dongsoop.dongsoop.report.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class ChatMessageSnapshotsConverter implements AttributeConverter<ChatMessageSnapshots, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final TypeReference<List<ChatMessageSnapshot>> SNAPSHOT_LIST = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(ChatMessageSnapshots attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(attribute.messages());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("신고 맥락 메시지를 저장할 수 없습니다.", e);
        }
    }

    @Override
    public ChatMessageSnapshots convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        try {
            return new ChatMessageSnapshots(OBJECT_MAPPER.readValue(dbData, SNAPSHOT_LIST));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("신고 맥락 메시지를 읽을 수 없습니다.", e);
        }
    }
}
