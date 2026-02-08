package com.dongsoop.dongsoop.blinddate.repository;

import com.dongsoop.dongsoop.blinddate.entity.SessionInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SessionInfoRepositoryImpl implements SessionInfoRepository {

    private final ParticipantInfoRepository participantInfoRepository;
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    /**
     * 세션 생성
     */
    public SessionInfo create() {
        SessionInfo session = SessionInfo.create();
        sessions.put(session.getSessionId(), session);
        log.info("Session created: sessionId={}", session.getSessionId());
        return session;
    }

    /**
     * 세션 상태 조회
     */
    public SessionState getState(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        return session != null ? session.getState() : null;
    }

    /**
     * 세션 시작
     */
    public void start(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        if (session != null) {
            session.start();
            log.info("Session started: sessionId={}", sessionId);
        }
    }

    /**
     * 세션 종료
     */
    public void terminate(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        participantInfoRepository.clearSession(sessionId);
        if (session != null) {
            session.terminate();
            log.info("Session terminated: sessionId={}", sessionId);
        }
    }

    public void clear() {
        this.sessions.clear();
    }
}
