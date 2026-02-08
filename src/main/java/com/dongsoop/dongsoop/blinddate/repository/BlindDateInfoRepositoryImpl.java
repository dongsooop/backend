package com.dongsoop.dongsoop.blinddate.repository;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class BlindDateInfoRepositoryImpl implements BlindDateInfoRepository {

    @Getter
    private volatile boolean available = false;      // 운영 중 여부

    @Getter
    private volatile Integer maxSessionMemberCount;  // 세션당 최대 인원수

    private volatile LocalDateTime expiredDate;      // 종료 시점
    private volatile String currentPointer;          // 현재 대기 세션 ID

    public void start(Integer maxSessionMemberCount, LocalDateTime expiredDate) {
        this.currentPointer = null; // 포인터를 먼저 초기화
        this.available = true;
        this.maxSessionMemberCount = maxSessionMemberCount;
        this.expiredDate = expiredDate;
        log.info("[BlindDate] started: maxCount={}, expiredDate={}", maxSessionMemberCount, expiredDate);
    }

    public void close() {
        this.available = false;
        this.currentPointer = null;
        this.maxSessionMemberCount = null;
        this.expiredDate = null;
        log.info("[BlindDate] closed");
    }

    public String getPointer() {
        return currentPointer;
    }

    public void setPointer(String sessionId) {
        this.currentPointer = sessionId;
    }
}
