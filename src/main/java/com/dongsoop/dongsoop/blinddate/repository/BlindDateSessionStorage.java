package com.dongsoop.dongsoop.blinddate.repository;

import com.dongsoop.dongsoop.blinddate.entity.SessionInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;

public interface BlindDateSessionStorage {

    SessionInfo create();

    SessionState getState(String sessionId);

    void start(String sessionId);

    void terminate(String sessionId);

    void clear();

    boolean isWaiting(String sessionId);
}
