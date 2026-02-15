package com.dongsoop.dongsoop.blinddate.repository;

import java.time.LocalDateTime;

public interface BlindDateStorage {

    void start(Integer maxSessionMemberCount, LocalDateTime expiredDate);

    void stop();

    void close();

    String getPointer();

    void setPointer(String sessionId);

    boolean isAvailable();

    Integer getMaxSessionMemberCount();
}
