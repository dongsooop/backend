package com.dongsoop.dongsoop.blinddate.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.event.BlindDateSessionClosedEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlindDateSessionMessageTest {

    private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 9, 28, 21, 0);

    private final List<Object> events = new ArrayList<>();
    private final BlindDateSessionStorageImpl sessions = new BlindDateSessionStorageImpl(events::add);
    private String sessionId;

    @BeforeEach
    void setUp() {
        sessionId = sessions.create().getSessionId();
    }

    private void record(String sessionId, String messageId) {
        sessions.recordMessage(sessionId, new BlindDateMessage(messageId, 1L, "내용" + messageId, SENT_AT));
    }

    @Test
    @DisplayName("세션의 메시지를 ID로 조회한다")
    void findMessage_ReturnsRecorded() {
        record(sessionId, "m1");

        assertThat(sessions.findMessage(sessionId, "m1")).map(BlindDateMessage::content).contains("내용m1");
        assertThat(sessions.findMessage(sessionId, "m2")).isEmpty();
    }

    @Test
    @DisplayName("직전 메시지는 지정한 메시지 앞의 것만 오래된 순으로 최대 limit개 반환한다")
    void findMessagesBefore_ReturnsOnlyPrecedingInOrder() {
        IntStream.range(0, 6).forEach(i -> record(sessionId, "m" + i));

        assertThat(sessions.findMessagesBefore(sessionId, "m4", 3)).extracting(BlindDateMessage::messageId)
                .containsExactly("m1", "m2", "m3");
        assertThat(sessions.findMessagesBefore(sessionId, "m0", 3)).isEmpty();
        assertThat(sessions.findMessagesBefore(sessionId, "missing", 3)).isEmpty();
    }

    @Test
    @DisplayName("다음 메시지는 지정한 메시지 뒤의 것만 오래된 순으로 최대 limit개 반환한다")
    void findMessagesAfter_ReturnsOnlyFollowingInOrder() {
        IntStream.range(0, 6).forEach(i -> record(sessionId, "m" + i));

        assertThat(sessions.findMessagesAfter(sessionId, "m1", 3)).extracting(BlindDateMessage::messageId)
                .containsExactly("m2", "m3", "m4");
        assertThat(sessions.findMessagesAfter(sessionId, "m5", 3)).isEmpty();
        assertThat(sessions.findMessagesAfter(sessionId, "missing", 3)).isEmpty();
        assertThat(sessions.findMessagesAfter("no-session", "m1", 3)).isEmpty();
    }

    @Test
    @DisplayName("세션당 최근 1,000개만 남기고 오래된 메시지부터 버린다")
    void recordMessage_KeepsLatestThousand() {
        IntStream.range(0, 1001).forEach(i -> record(sessionId, "m" + i));

        assertThat(sessions.findMessage(sessionId, "m0")).isEmpty();
        assertThat(sessions.findMessage(sessionId, "m1")).isPresent();
        assertThat(sessions.findMessagesBefore(sessionId, "m1000", 2000)).hasSize(999);
    }

    @Test
    @DisplayName("세션별로 따로 기록한다")
    void recordMessage_SeparatedBySession() {
        String otherSessionId = sessions.create().getSessionId();
        record(otherSessionId, "m1");

        assertThat(sessions.findMessage(sessionId, "m1")).isEmpty();
        assertThat(sessions.findMessage(otherSessionId, "m1")).isPresent();
    }

    @Test
    @DisplayName("세션이 종료되면 메시지 기록도 사라지고 이후 메시지는 기록하지 않는다")
    void terminate_RemovesMessages() {
        record(sessionId, "m1");

        sessions.terminate(sessionId);
        record(sessionId, "m2");

        assertThat(sessions.findMessage(sessionId, "m1")).isEmpty();
        assertThat(sessions.findMessage(sessionId, "m2")).isEmpty();
    }

    @Test
    @DisplayName("기록이 있는 세션을 종료하면 지우기 전의 메시지 기록으로 종료 이벤트를 한 번 발행한다")
    void terminate_PublishesClosedEventWithMessages() {
        record(sessionId, "m1");
        record(sessionId, "m2");

        sessions.terminate(sessionId);
        sessions.terminate(sessionId);

        assertThat(events).singleElement().isInstanceOfSatisfying(BlindDateSessionClosedEvent.class, event -> {
            assertThat(event.sessionId()).isEqualTo(sessionId);
            assertThat(event.messages()).extracting(BlindDateMessage::messageId).containsExactly("m1", "m2");
        });
    }

    @Test
    @DisplayName("기록이 없는 세션을 종료하면 이벤트를 발행하지 않는다")
    void terminate_WithoutMessages_PublishesNothing() {
        sessions.terminate(sessionId);

        assertThat(events).isEmpty();
    }

    @Test
    @DisplayName("전체 초기화하면 기록이 있는 세션마다 종료 이벤트를 발행하고 세션을 모두 지운다")
    void clear_PublishesClosedEventPerSessionWithMessages() {
        String silentSessionId = sessions.create().getSessionId();
        String otherSessionId = sessions.create().getSessionId();
        record(sessionId, "m1");
        record(otherSessionId, "o1");

        sessions.clear();

        assertThat(events).hasSize(2).allSatisfy(event -> assertThat(event).isInstanceOf(BlindDateSessionClosedEvent.class));
        assertThat(events).extracting(event -> ((BlindDateSessionClosedEvent) event).sessionId())
                .containsExactlyInAnyOrder(sessionId, otherSessionId);
        assertThat(sessions.getState(silentSessionId)).isNull();
        assertThat(sessions.getState(otherSessionId)).isNull();
    }

    @Test
    @DisplayName("이벤트 발행이 실패해도 세션 종료는 끝난다")
    void terminate_PublishFailure_StillRemovesSession() {
        BlindDateSessionStorageImpl failing = new BlindDateSessionStorageImpl(event -> {
            throw new IllegalStateException("boom");
        });
        String failingSessionId = failing.create().getSessionId();
        failing.recordMessage(failingSessionId, new BlindDateMessage("m1", 1L, "내용", SENT_AT));

        failing.terminate(failingSessionId);

        assertThat(failing.getState(failingSessionId)).isNull();
    }

    @Test
    @DisplayName("전체 초기화하면 메시지 기록도 사라진다")
    void clear_RemovesMessages() {
        record(sessionId, "m1");

        sessions.clear();

        assertThat(sessions.findMessage(sessionId, "m1")).isEmpty();
    }
}
