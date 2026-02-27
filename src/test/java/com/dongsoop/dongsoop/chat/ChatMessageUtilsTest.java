package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.util.ChatMessageUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageUtilsTest {

    @Test
    @DisplayName("generateMessageId - UUID 형식의 non-null 문자열을 반환한다")
    void generateMessageId_returnsValidUuidFormat() {
        String messageId = ChatMessageUtils.generateMessageId();

        assertNotNull(messageId);
        assertDoesNotThrow(() -> UUID.fromString(messageId));
    }

    @Test
    @DisplayName("getCurrentTime - non-null LocalDateTime을 반환한다")
    void getCurrentTime_returnsNonNull() {
        LocalDateTime time = ChatMessageUtils.getCurrentTime();

        assertNotNull(time);
    }

    @Test
    @DisplayName("validatePositiveUserId - null이면 UnauthorizedChatAccessException을 던진다")
    void validatePositiveUserId_throwsOnNull() {
        assertThrows(UnauthorizedChatAccessException.class,
                () -> ChatMessageUtils.validatePositiveUserId(null));
    }

    @Test
    @DisplayName("validatePositiveUserId - 음수이면 UnauthorizedChatAccessException을 던진다")
    void validatePositiveUserId_throwsOnNegative() {
        assertThrows(UnauthorizedChatAccessException.class,
                () -> ChatMessageUtils.validatePositiveUserId(-1L));
    }

    @Test
    @DisplayName("validatePositiveUserId - 양수이면 예외를 던지지 않는다")
    void validatePositiveUserId_succeedsOnPositive() {
        assertDoesNotThrow(() -> ChatMessageUtils.validatePositiveUserId(1L));
    }

    @Test
    @DisplayName("enrichMessage - messageId가 null이면 자동으로 설정한다")
    void enrichMessage_setsMessageIdWhenNull() {
        ChatMessage message = ChatMessage.builder()
                .roomId("room-1")
                .senderId(1L)
                .content("hello")
                .build();

        ChatMessageUtils.enrichMessage(message);

        assertNotNull(message.getMessageId());
        assertDoesNotThrow(() -> UUID.fromString(message.getMessageId()));
    }

    @Test
    @DisplayName("enrichMessage - timestamp가 null이면 자동으로 설정한다")
    void enrichMessage_setsTimestampWhenNull() {
        ChatMessage message = ChatMessage.builder()
                .roomId("room-1")
                .senderId(1L)
                .content("hello")
                .build();

        ChatMessageUtils.enrichMessage(message);

        assertNotNull(message.getTimestamp());
    }

    @Test
    @DisplayName("enrichMessage - type이 null이면 CHAT으로 설정한다")
    void enrichMessage_setsTypeToChatWhenNull() {
        ChatMessage message = ChatMessage.builder()
                .roomId("room-1")
                .senderId(1L)
                .content("hello")
                .build();

        ChatMessageUtils.enrichMessage(message);

        assertEquals(MessageType.CHAT, message.getType());
    }

    @Test
    @DisplayName("enrichMessage - 기존 값이 있으면 덮어쓰지 않는다")
    void enrichMessage_preservesExistingValues() {
        String existingId = "existing-id";
        LocalDateTime existingTime = LocalDateTime.of(2025, 1, 1, 12, 0);
        MessageType existingType = MessageType.ENTER;

        ChatMessage message = ChatMessage.builder()
                .messageId(existingId)
                .roomId("room-1")
                .senderId(1L)
                .content("hello")
                .timestamp(existingTime)
                .type(existingType)
                .build();

        ChatMessageUtils.enrichMessage(message);

        assertEquals(existingId, message.getMessageId());
        assertEquals(existingTime, message.getTimestamp());
        assertEquals(existingType, message.getType());
    }

    @Test
    @DisplayName("enrichMessage - null 메시지를 전달하면 예외 없이 무시한다")
    void enrichMessage_handlesNullMessageGracefully() {
        assertDoesNotThrow(() -> ChatMessageUtils.enrichMessage(null));
    }
}
