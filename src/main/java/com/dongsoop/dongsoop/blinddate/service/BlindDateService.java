package com.dongsoop.dongsoop.blinddate.service;

import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;

public interface BlindDateService {

    boolean isAvailable();

    void startBlindDate(StartBlindDateRequest request);

    void broadcastJoinedCount(String sessionId, int count);
}
