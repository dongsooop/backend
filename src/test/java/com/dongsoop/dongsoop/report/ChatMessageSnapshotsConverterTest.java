package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshotsConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatMessageSnapshotsConverterTest {

    private final ChatMessageSnapshotsConverter converter = new ChatMessageSnapshotsConverter();

    @Test
    @DisplayName("맥락 메시지를 JSON 문자열로 저장하고 그대로 복원한다")
    void roundTrip() {
        ChatMessageSnapshots snapshots = new ChatMessageSnapshots(List.of(
                ChatMessageSnapshot.of(1L, "안녕", LocalDateTime.of(2026, 9, 28, 21, 0))));

        String json = converter.convertToDatabaseColumn(snapshots);

        assertThat(json).contains("\"2026-09-28T21:00:00\"");
        assertThat(converter.convertToEntityAttribute(json)).isEqualTo(snapshots);
    }

    @Test
    @DisplayName("null은 null로 저장한다")
    void nullStaysNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("관리자 API 응답에서는 배열로 직렬화된다")
    void serializesAsArray() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ChatMessageSnapshots snapshots = new ChatMessageSnapshots(List.of(ChatMessageSnapshot.of(1L, "a", null)));

        assertThat(objectMapper.writeValueAsString(snapshots)).startsWith("[");
    }

    @Test
    @DisplayName("1,000자를 넘는 내용은 잘리고 null은 빈 문자열이 된다")
    void truncate() {
        assertThat(ChatMessageSnapshot.truncate("가".repeat(1500))).hasSize(1000);
        assertThat(ChatMessageSnapshot.truncate(null)).isEmpty();
    }

    @Test
    @DisplayName("정확히 1,000자는 그대로 저장된다")
    void truncate_ExactlyMaxLength_StaysAsIs() {
        String content = "가".repeat(1000);

        assertThat(ChatMessageSnapshot.truncate(content)).isEqualTo(content);
    }

    @Test
    @DisplayName("1,001자는 1,000자로 잘린다")
    void truncate_OneOverMaxLength_CutsToMaxLength() {
        assertThat(ChatMessageSnapshot.truncate("가".repeat(1001))).hasSize(1000);
    }

    @Test
    @DisplayName("1,000번째 유닛이 상위 서로게이트면 서로게이트 쌍을 쪼개지 않도록 999자로 자른다")
    void truncate_SurrogatePairAtBoundary_DoesNotSplitPair() {
        String content = "가".repeat(999) + "😀" + "끝";

        String truncated = ChatMessageSnapshot.truncate(content);

        assertThat(truncated).hasSize(999);
        assertThat(Character.isHighSurrogate(truncated.charAt(truncated.length() - 1))).isFalse();
    }
}
