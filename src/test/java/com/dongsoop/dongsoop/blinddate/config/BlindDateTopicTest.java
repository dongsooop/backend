package com.dongsoop.dongsoop.blinddate.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * BlindDateTopic 유틸 클래스 단위 테스트
 */
@DisplayName("BlindDateTopic 단위 테스트")
class BlindDateTopicTest {

    @Test
    @DisplayName("sessionStart - 토픽 생성")
    void sessionStart_GeneratesTopic() {
        // when
        String topic = BlindDateTopic.sessionStart("session-123");

        // then
        assertThat(topic).isEqualTo("/topic/blinddate/session/session-123/start");
    }

    @Test
    @DisplayName("freeze - 토픽 생성")
    void freeze_GeneratesTopic() {
        // when
        String topic = BlindDateTopic.freeze("session-123");

        // then
        assertThat(topic).isEqualTo("/topic/blinddate/session/session-123/freeze");
    }

    @Test
    @DisplayName("thaw - 토픽 생성")
    void thaw_GeneratesTopic() {
        // when
        String topic = BlindDateTopic.thaw("session-123");

        // then
        assertThat(topic).isEqualTo("/topic/blinddate/session/session-123/thaw");
    }

    @Test
    @DisplayName("system - 토픽 생성")
    void system_GeneratesTopic() {
        // when
        String topic = BlindDateTopic.system("session-123");

        // then
        assertThat(topic).isEqualTo("/topic/blinddate/session/session-123/system");
    }

    @Test
    @DisplayName("message - 토픽 생성")
    void message_GeneratesTopic() {
        // when
        String topic = BlindDateTopic.message("session-123");

        // then
        assertThat(topic).isEqualTo("/topic/blinddate/session/session-123/message");
    }

    @Test
    @DisplayName("joined - 토픽 생성")
    void joined_GeneratesTopic() {
        // when
        String topic = BlindDateTopic.joined("session-123");

        // then
        assertThat(topic).isEqualTo("/topic/blinddate/session/session-123/joined");
    }

    @Test
    @DisplayName("participants - 토픽 생성")
    void participants_GeneratesTopic() {
        // when
        String topic = BlindDateTopic.participants("session-123");

        // then
        assertThat(topic).isEqualTo("/topic/blinddate/session/session-123/participants");
    }

    @Test
    @DisplayName("chatRoomCreated - 토픽 생성")
    void chatRoomCreated_GeneratesTopic() {
        // when
        String topic = BlindDateTopic.chatRoomCreated("session-123", 1L);

        // then
        assertThat(topic).isEqualTo("/topic/blinddate/session/session-123/member/1/chatroom");
    }

    @Test
    @DisplayName("matchFailed - 토픽 생성")
    void matchFailed_GeneratesTopic() {
        // when
        String topic = BlindDateTopic.matchFailed("session-123", 1L);

        // then
        assertThat(topic).isEqualTo("/topic/blinddate/session/session-123/member/1/failed");
    }

    @Test
    @DisplayName("다양한 sessionId - 토픽 생성")
    void variousSessionIds_GenerateTopics() {
        // when & then
        assertThat(BlindDateTopic.freeze("abc-123")).contains("abc-123");
        assertThat(BlindDateTopic.thaw("xyz-999")).contains("xyz-999");
        assertThat(BlindDateTopic.system("test-session")).contains("test-session");
    }

    @Test
    @DisplayName("다양한 memberId - 토픽 생성")
    void variousMemberIds_GenerateTopics() {
        // when & then
        assertThat(BlindDateTopic.chatRoomCreated("session-1", 1L)).contains("/member/1/");
        assertThat(BlindDateTopic.chatRoomCreated("session-1", 999L)).contains("/member/999/");
        assertThat(BlindDateTopic.matchFailed("session-1", 123L)).contains("/member/123/");
    }
}
