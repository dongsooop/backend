package com.dongsoop.dongsoop.blinddate.repository;

import java.time.LocalDateTime;

public interface BlindDateInfoRepository {

    void start(Integer maxSessionMemberCount, LocalDateTime expiredDate);

    void close();

    String getPointer();

    void setPointer(String sessionId);
}
