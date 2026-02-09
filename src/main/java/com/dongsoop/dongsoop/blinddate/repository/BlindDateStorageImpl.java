package com.dongsoop.dongsoop.blinddate.repository;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class BlindDateStorageImpl implements BlindDateStorage {

    private volatile boolean available = false;      // 운영 중 여부
    private volatile Integer maxSessionMemberCount;  // 세션당 최대 인원수
    private volatile String currentPointer;          // 현재 대기 세션 ID

    public void start(Integer maxSessionMemberCount, LocalDateTime expiredDate) {
        this.currentPointer = null;                         // 포인터 초기화
        this.maxSessionMemberCount = maxSessionMemberCount; // 세션 전원 초기화
        this.available = true;                              // 가장 나중에 available 초기화
        log.info("[BlindDate] started: maxCount={}, expiredDate={}", maxSessionMemberCount, expiredDate);
    }

    @Override
    public synchronized void stop() {
        this.available = false;
    }

    public synchronized void close() {
        this.available = false;             // 가장 먼저 available 종료
        this.currentPointer = null;         // 포인터 초기화
        this.maxSessionMemberCount = null;  // 세션 정원 초기화
        log.info("[BlindDate] closed");
    }

    @Override
    public String getPointer() {
        return currentPointer;
    }

    @Override
    public synchronized void setPointer(String sessionId) {
        this.currentPointer = sessionId;
    }

    /**
     * 과팅 운영 여부 조회
     *
     * @return 과팅 운영 여부
     */
    @Override
    public boolean isAvailable() {
        return this.available;
    }

    /**
     * 세션별 정원 수 조회
     *
     * @return 세션별 정원 수
     */
    @Override
    public Integer getMaxSessionMemberCount() {
        return this.maxSessionMemberCount;
    }
}
