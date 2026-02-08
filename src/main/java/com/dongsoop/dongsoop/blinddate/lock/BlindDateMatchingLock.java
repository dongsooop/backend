package com.dongsoop.dongsoop.blinddate.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

@Component
public class BlindDateMatchingLock {

    // 매칭 락
    private final Lock matchingLocks = new ReentrantLock();

    /**
     * 매칭 락 획득
     */
    public void lock() {
        this.matchingLocks.lock();
    }

    public void unlock() {
        this.matchingLocks.unlock();
    }
}
