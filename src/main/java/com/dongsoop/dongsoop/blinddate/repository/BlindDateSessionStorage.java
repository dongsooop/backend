package com.dongsoop.dongsoop.blinddate.repository;

import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import java.util.List;
import java.util.Optional;

public interface BlindDateSessionStorage {

    SessionInfo create();

    SessionState getState(String sessionId);

    void start(String sessionId);

    void terminate(String sessionId);

    void clear();

    boolean isWaiting(String sessionId);

    boolean isProcessing(String sessionId);

    /** 세션이 없으면(종료·초기화) 기록하지 않는다. */
    void recordMessage(String sessionId, BlindDateMessage message);

    Optional<BlindDateMessage> findMessage(String sessionId, String messageId);

    /** 지정한 메시지 직전의 메시지를 오래된 순으로 최대 limit개 반환한다. */
    List<BlindDateMessage> findMessagesBefore(String sessionId, String messageId, int limit);
}
