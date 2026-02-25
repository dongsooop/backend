package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.session.WebSocketSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class WebSocketSessionManagerTest {

    private WebSocketSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new WebSocketSessionManager();
    }

    @Test
    @DisplayName("addUserSession 호출 후 isUserOnline이 true를 반환한다")
    void addUserSession_then_isUserOnline_returns_true() {
        sessionManager.addUserSession(1L, "session-1");

        assertThat(sessionManager.isUserOnline(1L)).isTrue();
    }

    @Test
    @DisplayName("등록되지 않은 사용자에 대해 isUserOnline이 false를 반환한다")
    void isUserOnline_returns_false_for_unknown_user() {
        assertThat(sessionManager.isUserOnline(999L)).isFalse();
    }

    @Test
    @DisplayName("removeSession 호출 후 해당 사용자의 isUserOnline이 false를 반환한다")
    void removeSession_removes_user() {
        sessionManager.addUserSession(1L, "session-1");

        sessionManager.removeSession("session-1");

        assertThat(sessionManager.isUserOnline(1L)).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 sessionId로 removeSession을 호출해도 예외가 발생하지 않는다")
    void removeSession_with_unknown_sessionId_does_not_throw() {
        assertThatCode(() -> sessionManager.removeSession("unknown-session"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("addUserSession 후 removeSession을 호출하면 isUserOnline이 false를 반환한다")
    void addUserSession_then_removeSession_then_isUserOnline_returns_false() {
        sessionManager.addUserSession(1L, "session-1");
        sessionManager.removeSession("session-1");

        assertThat(sessionManager.isUserOnline(1L)).isFalse();
    }
}
