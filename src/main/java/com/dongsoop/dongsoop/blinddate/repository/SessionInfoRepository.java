package com.dongsoop.dongsoop.blinddate.repository;

import com.dongsoop.dongsoop.blinddate.entity.SessionInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;

public interface SessionInfoRepository {
    /**
     * 세션 생성 (Lock 사용)
     */
    SessionInfo create();

    /**
     * 세션 상태 조회
     */
    SessionState getState(String sessionId);

    /**
     * 세션 시작
     */
    void start(String sessionId);

    /**
     * 세션 종료
     */
    void terminate(String sessionId);

    void clear();
}
