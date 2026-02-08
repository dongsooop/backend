package com.dongsoop.dongsoop.blinddate.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

@Component
public class BlindDateSessionLock {

    // 세션 락
    private final Map<String, Lock> sessionLocks = new ConcurrentHashMap<>();

    /**
     * 세션별 락 획득
     *
     * @param sessionId 락을 획득할 세션 id
     */
    public void lockBySessionId(String sessionId) {
        Lock lock = sessionLocks.computeIfAbsent(sessionId, k -> new ReentrantLock());
        lock.lock();
    }

    /**
     * 세션 락 반납
     *
     * @param sessionId 락을 반납할 세션 id
     */
    public void unlockBySessionId(String sessionId) {
        Lock lock = sessionLocks.computeIfAbsent(sessionId, k -> new ReentrantLock());
        lock.unlock();
    }

    /**
     * 세션 종료 후 락 클리어
     */
    public void clear() {
        this.sessionLocks.clear();
    }
}
