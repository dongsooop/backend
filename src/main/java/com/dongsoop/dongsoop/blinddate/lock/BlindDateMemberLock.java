package com.dongsoop.dongsoop.blinddate.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

@Component
public class BlindDateMemberLock {

    // 회원 락
    private final Map<Long, Lock> memberLocks = new ConcurrentHashMap<>();

    /**
     * 회원별 락 획득
     *
     * @param memberId 락을 획득할 회원 id
     */
    public void lockByMemberId(Long memberId) {
        Lock lock = memberLocks.computeIfAbsent(memberId, k -> new ReentrantLock());
        lock.lock();
    }

    /**
     * 회원 락 반납
     *
     * @param memberId 락을 반납할 회원 id
     */
    public void unlockByMemberId(Long memberId) {
        Lock lock = memberLocks.computeIfAbsent(memberId, k -> new ReentrantLock());
        lock.unlock();
    }

    /**
     * 세션 종료 후 락 클리어
     */
    public void clear() {
        this.memberLocks.clear();
    }
}
