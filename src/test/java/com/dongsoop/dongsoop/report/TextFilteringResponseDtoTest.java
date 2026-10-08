package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.report.dto.TextFilteringResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextFilteringResponseDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("content에만 비속어가 있으면 hasProfanity가 true다")
    void hasProfanity_ContentOnly_ReturnsTrue() throws Exception {
        String json = "{\"title\":{\"has_profanity\":false},\"tags\":{\"has_profanity\":false},"
                + "\"content\":{\"has_profanity\":true}}";

        TextFilteringResponseDto dto = objectMapper.readValue(json, TextFilteringResponseDto.class);

        assertThat(dto.hasProfanity()).isTrue();
    }

    @Test
    @DisplayName("모든 필드가 false면 hasProfanity가 false다")
    void hasProfanity_AllFalse_ReturnsFalse() throws Exception {
        String json = "{\"title\":{\"has_profanity\":false},\"tags\":{\"has_profanity\":false},"
                + "\"content\":{\"has_profanity\":false}}";

        TextFilteringResponseDto dto = objectMapper.readValue(json, TextFilteringResponseDto.class);

        assertThat(dto.hasProfanity()).isFalse();
    }

    @Test
    @DisplayName("title만 true여도 hasProfanity가 true다")
    void hasProfanity_TitleOnly_ReturnsTrue() throws Exception {
        String json = "{\"title\":{\"has_profanity\":true},\"tags\":{\"has_profanity\":false},"
                + "\"content\":{\"has_profanity\":false}}";

        TextFilteringResponseDto dto = objectMapper.readValue(json, TextFilteringResponseDto.class);

        assertThat(dto.hasProfanity()).isTrue();
    }

    @Test
    @DisplayName("알 수 없는 필드가 섞여 있어도 역직렬화된다")
    void hasProfanity_WithUnknownFields_StillDeserializes() throws Exception {
        String json = "{\"title\":{\"has_profanity\":false,\"score\":0.1},\"tags\":{\"has_profanity\":false},"
                + "\"content\":{\"has_profanity\":true},\"model\":\"v1\"}";

        TextFilteringResponseDto dto = objectMapper.readValue(json, TextFilteringResponseDto.class);

        assertThat(dto.hasProfanity()).isTrue();
    }
}
