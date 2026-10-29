package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.report.dto.TextFilteringResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TextFilteringResponseDtoTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @ParameterizedTest(name = "title={0}, tags={1}, content={2} → hasProfanity={3}")
  @CsvSource({"false, false, true, true", "false, false, false, false", "true, false, false, true"})
  @DisplayName("필드 하나라도 비속어가 있으면 hasProfanity가 true다")
  void hasProfanity_ReturnsAggregatedResult(
      boolean titleProfanity, boolean tagsProfanity, boolean contentProfanity, boolean expected)
      throws Exception {
    String json =
        "{\"title\":{\"has_profanity\":%s},\"tags\":{\"has_profanity\":%s},"
                .formatted(titleProfanity, tagsProfanity)
            + "\"content\":{\"has_profanity\":%s}}".formatted(contentProfanity);

    TextFilteringResponseDto dto = objectMapper.readValue(json, TextFilteringResponseDto.class);

    assertThat(dto.hasProfanity()).isEqualTo(expected);
  }

  @Test
  @DisplayName("알 수 없는 필드가 섞여 있어도 역직렬화된다")
  void hasProfanity_WithUnknownFields_StillDeserializes() throws Exception {
    String json =
        "{\"title\":{\"has_profanity\":false,\"score\":0.1},\"tags\":{\"has_profanity\":false},"
            + "\"content\":{\"has_profanity\":true},\"model\":\"v1\"}";

    TextFilteringResponseDto dto = objectMapper.readValue(json, TextFilteringResponseDto.class);

    assertThat(dto.hasProfanity()).isTrue();
  }
}
