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

    void recordMessage(String sessionId, BlindDateMessage message);

    Optional<BlindDateMessage> findMessage(String sessionId, String messageId);

    List<BlindDateMessage> findMessagesBefore(String sessionId, String messageId, int limit);
}
