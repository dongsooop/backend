package com.dongsoop.dongsoop.blinddate.entity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 과팅 참여 정보 - 한 사용자는 여러 소켓으로 접속 가능 - 하지만 한 세션에만 소속
 */
@Getter
@Builder
@AllArgsConstructor
public class ParticipantInfo {
    private final String sessionId;        // 세션 ID
    private final Long memberId;           // 사용자 ID
    private final Set<String> socketIds;   // 소켓 ID 목록 (여러 기기 접속 가능)
    private final String anonymousName;    // 익명 이름
    private final LocalDateTime joinedAt;

    public static ParticipantInfo create(String sessionId, Long memberId, String socketId, String anonymousName) {
        Set<String> socketIds = ConcurrentHashMap.newKeySet();
        socketIds.add(socketId);

        return ParticipantInfo.builder()
                .sessionId(sessionId)
                .memberId(memberId)
                .socketIds(socketIds)
                .anonymousName(anonymousName)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 소켓 추가 (같은 사용자가 다른 기기로 접속)
     */
    public void addSocket(String socketId) {
        this.socketIds.add(socketId);
    }

    /**
     * 소켓 제거 (한 기기 연결 해제)
     */
    public boolean removeSocket(String socketId) {
        return this.socketIds.remove(socketId);
    }

    /**
     * 모든 소켓 연결 해제 확인
     */
    public boolean hasNoSockets() {
        return this.socketIds.isEmpty();
    }

    /**
     * 읽기 전용 소켓 목록
     */
    public Set<String> getSocketIds() {
        return Collections.unmodifiableSet(socketIds);
    }
}
