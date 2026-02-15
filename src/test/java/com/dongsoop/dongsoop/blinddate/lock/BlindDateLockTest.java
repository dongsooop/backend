package com.dongsoop.dongsoop.blinddate.lock;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@DisplayName("BlindDate Lock 테스트")
class BlindDateLockTest {
    private static final Logger log = LoggerFactory.getLogger(BlindDateLockTest.class);
    private BlindDateMemberLock memberLock;
    private BlindDateSessionLock sessionLock;
    @BeforeEach
    void setUp() {
        memberLock = new BlindDateMemberLock();
        sessionLock = new BlindDateSessionLock();
    }
    @Nested
    @DisplayName("1. 멤버 Lock 기본 기능")
    class MemberLockBasicTests {
        @Test
        @DisplayName("Lock 획득 및 해제")
        void lockAndUnlock() {
            Long memberId = 1L;
            memberLock.lockByMemberId(memberId);
            memberLock.unlockByMemberId(memberId);
        }
        @Test
        @DisplayName("서로 다른 멤버 Lock - 독립적")
        void differentMembers_IndependentLocks() throws InterruptedException {
            Long member1 = 1L;
            Long member2 = 2L;
            CountDownLatch latch = new CountDownLatch(2);
            AtomicInteger executionOrder = new AtomicInteger(0);
            List<Integer> order = Collections.synchronizedList(new ArrayList<>());
            Thread thread1 = new Thread(() -> {
                memberLock.lockByMemberId(member1);
                try {
                    order.add(executionOrder.incrementAndGet());
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.error("Thread interrupted", e);
                } finally {
                    memberLock.unlockByMemberId(member1);
                    latch.countDown();
                }
            });
            Thread thread2 = new Thread(() -> {
                memberLock.lockByMemberId(member2);
                try {
                    order.add(executionOrder.incrementAndGet());
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.error("Thread interrupted", e);
                } finally {
                    memberLock.unlockByMemberId(member2);
                    latch.countDown();
                }
            });
            thread1.start();
            thread2.start();
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(order).hasSize(2);
        }
    }
}
