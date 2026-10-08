package com.dongsoop.dongsoop.blinddate.entity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionInfo {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // 세션이 짧아 실제로는 닿지 않는 상한. 메모리 폭주만 막는다
    private static final int MAX_MESSAGES = 1000;
    private final String sessionId;
    private final LocalDateTime createdAt;
    private volatile SessionState state;

    // 신고 근거로 쓰는 사용자 메시지. 세션 종료로 SessionInfo가 지워질 때 함께 사라진다
    @Getter(AccessLevel.NONE)
    @Builder.Default
    private final Deque<BlindDateMessage> messages = new ArrayDeque<>();

    public static SessionInfo create() {
        return SessionInfo.builder()
                .sessionId(UUID.randomUUID().toString())
                .state(SessionState.WAITING)
                .createdAt(LocalDateTime.now(KST))
                .build();
    }

    public void start() {
        this.state = SessionState.PROCESSING;
    }

    public boolean isProcessing() {
        return this.state == SessionState.PROCESSING;
    }

    public boolean isWaiting() {
        return this.state == SessionState.WAITING;
    }

    public synchronized void recordMessage(BlindDateMessage message) {
        messages.addLast(message);
        if (messages.size() > MAX_MESSAGES) {
            messages.removeFirst();
        }
    }

    public synchronized Optional<BlindDateMessage> findMessage(String messageId) {
        return messages.stream()
                .filter(message -> message.messageId().equals(messageId))
                .findFirst();
    }

    /**
     * 지정한 메시지 직전의 메시지를 오래된 순으로 최대 limit개 반환한다. 지정한 메시지가 없으면 빈 목록이다.
     */
    public synchronized List<BlindDateMessage> findMessagesBefore(String messageId, int limit) {
        List<BlindDateMessage> before = new ArrayList<>();
        for (BlindDateMessage message : messages) {
            if (message.messageId().equals(messageId)) {
                return List.copyOf(before.subList(Math.max(0, before.size() - limit), before.size()));
            }
            before.add(message);
        }
        return List.of();
    }

    public enum SessionState {
        WAITING,     // 대기 중
        PROCESSING  // 진행 중
        // 종료 시 삭제됨으로 상태를 가지지 않음
    }
}
