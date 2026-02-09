package com.dongsoop.dongsoop.oauth.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

@Component
public class SocialAccountLockManager {

    private final Map<String, Lock> socialAccountLocks = new ConcurrentHashMap<>();

    public void lockBySocialAccount(String socialAccount) {
        Lock lock = this.socialAccountLocks.computeIfAbsent(socialAccount, (key) -> new ReentrantLock());
        lock.lock();
    }

    public void unlockBySocialAccount(String socialAccount) {
        Lock lock = this.socialAccountLocks.get(socialAccount);
        if (lock != null) {
            lock.unlock();
        }
    }
}
