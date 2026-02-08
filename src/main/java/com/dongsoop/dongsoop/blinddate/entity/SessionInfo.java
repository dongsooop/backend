package com.dongsoop.dongsoop.blinddate.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionInfo {
    private final String sessionId;
    private final LocalDateTime createdAt;
    private SessionState state;

    public static SessionInfo create() {
        return SessionInfo.builder()
                .sessionId(UUID.randomUUID().toString())
                .state(SessionState.WAITING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void start() {
        this.state = SessionState.PROCESSING;
    }

    public boolean isProcessing() {
        return this.state == SessionState.PROCESSING;
    }

    public enum SessionState {
        WAITING,     // 대기 중
        PROCESSING  // 진행 중
        // 종료 시 삭제됨으로 상태를 가지지 않음
    }
}
