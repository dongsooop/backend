package com.dongsoop.dongsoop.blinddate.service;

import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;

public interface BlindDateService {

  boolean isAvailable();

  void startBlindDate(StartBlindDateRequest request);

  void resetParticipants();

  void broadcastJoinedCount(String sessionId, int count);

  /** 참가자를 세션에서 내보내고 이번 회차 재입장을 막는다. 처리는 참여 큐에서 순서대로 한다. */
  void kickParticipant(String sessionId, Long memberId);
}
