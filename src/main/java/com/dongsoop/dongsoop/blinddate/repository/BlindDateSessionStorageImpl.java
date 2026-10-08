package com.dongsoop.dongsoop.blinddate.repository;

import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import com.dongsoop.dongsoop.blinddate.event.BlindDateSessionClosedEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BlindDateSessionStorageImpl implements BlindDateSessionStorage {

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 세션 생성
     *
     * @return 생성된 세션 정보
     */
    @Override
    public SessionInfo create() {
        SessionInfo session = SessionInfo.create();
        sessions.put(session.getSessionId(), session);
        log.info("Session created: sessionId={}", session.getSessionId());
        return session;
    }

    /**
     * 세션 상태 조회
     *
     * @param sessionId 조회할 세션 id
     * @return 세션 상태
     */
    @Override
    public SessionState getState(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        return session != null ? session.getState() : null;
    }

    /**
     * 세션 시작
     *
     * @param sessionId 시작할 세션 id
     */
    @Override
    public void start(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        if (session != null) {
            session.start();
            log.info("Session started: sessionId={}", sessionId);
        }
    }

    /**
     * 세션 종료
     *
     * @param sessionId 종료할 세션 id
     */
    @Override
    public void terminate(String sessionId) {
        SessionInfo session = this.sessions.remove(sessionId); // 종료된 세션 정보 제거
        if (session != null) {
            log.info("[BlindDate] Session terminated: sessionId={}", sessionId);
            publishClosed(session);
        }
    }

    /**
     * 세션 전체 삭제
     */
    @Override
    public void clear() {
        for (String sessionId : this.sessions.keySet()) {
            SessionInfo session = this.sessions.remove(sessionId);
            if (session != null) {
                publishClosed(session);
            }
        }
    }

    // 세션 종료·초기화가 이벤트 발행 실패로 중단되면 안 된다
    private void publishClosed(SessionInfo session) {
        List<BlindDateMessage> messages = session.messages();
        if (messages.isEmpty()) {
            return;
        }

        try {
            eventPublisher.publishEvent(new BlindDateSessionClosedEvent(session.getSessionId(), messages));
        } catch (RuntimeException e) {
            log.error("[BlindDate] Session closed event failed: sessionId={}", session.getSessionId(), e);
        }
    }

    @Override
    public boolean isWaiting(String sessionId) {
        SessionInfo sessionInfo = this.sessions.get(sessionId);
        if (sessionInfo == null) {
            return false;
        }

        return sessionInfo.isWaiting();
    }

    @Override
    public boolean isProcessing(String sessionId) {
        SessionInfo sessionInfo = this.sessions.get(sessionId);
        if (sessionInfo == null) {
            return false;
        }

        return sessionInfo.isProcessing();
    }

    @Override
    public void recordMessage(String sessionId, BlindDateMessage message) {
        SessionInfo sessionInfo = this.sessions.get(sessionId);
        if (sessionInfo != null) {
            sessionInfo.recordMessage(message);
        }
    }

    @Override
    public Optional<BlindDateMessage> findMessage(String sessionId, String messageId) {
        return Optional.ofNullable(this.sessions.get(sessionId))
                .flatMap(sessionInfo -> sessionInfo.findMessage(messageId));
    }

    @Override
    public List<BlindDateMessage> findMessagesBefore(String sessionId, String messageId, int limit) {
        SessionInfo sessionInfo = this.sessions.get(sessionId);
        if (sessionInfo == null) {
            return List.of();
        }

        return sessionInfo.findMessagesBefore(messageId, limit);
    }

    @Override
    public List<BlindDateMessage> findMessagesAfter(String sessionId, String messageId, int limit) {
        SessionInfo sessionInfo = this.sessions.get(sessionId);
        if (sessionInfo == null) {
            return List.of();
        }

        return sessionInfo.findMessagesAfter(messageId, limit);
    }
}
