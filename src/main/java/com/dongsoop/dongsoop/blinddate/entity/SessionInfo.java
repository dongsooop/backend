package com.dongsoop.dongsoop.blinddate.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * 과팅 세션 정보 (상태만 관리, 참가자 수는 ParticipantInfo에서 조회)
 */
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

    public void terminate() {
        this.state = SessionState.ENDED;
    }


    public boolean isWaiting() {
        return this.state == SessionState.WAITING;
    }

    public boolean isProcessing() {
        return this.state == SessionState.PROCESSING;
    }

    public boolean isEnded() {
        return this.state == SessionState.ENDED;
    }

    public enum SessionState {
        WAITING,     // 대기 중
        PROCESSING,  // 진행 중
        ENDED        // 종료
    }
}
