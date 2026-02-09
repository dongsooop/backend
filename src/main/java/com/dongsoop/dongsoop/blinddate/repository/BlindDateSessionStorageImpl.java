package com.dongsoop.dongsoop.blinddate.repository;

import com.dongsoop.dongsoop.blinddate.entity.SessionInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class BlindDateSessionStorageImpl implements BlindDateSessionStorage {

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

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
        SessionInfo session = sessions.get(sessionId);
        if (session != null) {
            this.sessions.remove(sessionId); // 종료된 세션 정보 제거
            log.info("[BlindDate] Session terminated: sessionId={}", sessionId);
        }
    }

    /**
     * 세션 전체 삭제
     */
    @Override
    public synchronized void clear() {
        this.sessions.clear();
    }

    @Override
    public synchronized boolean isWaiting(String sessionId) {
        SessionInfo sessionInfo = this.sessions.get(sessionId);
        if (sessionInfo == null) {
            return false;
        }

        return sessionInfo.isWaiting();
    }
}
