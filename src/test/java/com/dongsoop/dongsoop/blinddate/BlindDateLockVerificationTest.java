package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.blinddate.lock.BlindDateMemberLock;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateSessionLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lock 메커니즘 검증 테스트
 * <p>
 * 이 테스트는 실제 동시성 문제를 발견하기 위한 테스트입니다.
 */
@DisplayName("Lock 메커니즘 검증 테스트")
class BlindDateLockVerificationTest {

    private static final Logger log = LoggerFactory.getLogger(BlindDateLockVerificationTest.class);

    private BlindDateSessionLock sessionLock;
    private BlindDateMemberLock memberLock;

    @BeforeEach
    void setUp() {
        sessionLock = new BlindDateSessionLock();
        memberLock = new BlindDateMemberLock();
    }

    @RepeatedTest(20)
    @DisplayName("세션 락 동시 획득 - 순차적으로 실행되어야 함")
    void sessionLock_ConcurrentAccess_ShouldBeSequential() throws InterruptedException {
        String sessionId = "test-session";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작하도록

                    sessionLock.lockBySessionId(sessionId);
                    try {
                        int concurrent = currentConcurrent.incrementAndGet();
                        maxConcurrent.updateAndGet(max -> Math.max(max, concurrent));

                        // Critical section
                        int value = counter.get();
                        Thread.sleep(10); // 경합 조건 유도
                        counter.set(value + 1);

                        currentConcurrent.decrementAndGet();
                    } finally {
                        sessionLock.unlockBySessionId(sessionId);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Exception in thread {}", threadNum, e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드 시작
        assertThat(endLatch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        if (!exceptions.isEmpty()) {
            log.error("=== Lock Test Exceptions ===");
            exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
        }

        assertThat(exceptions).as("예외 없어야 함").isEmpty();
        assertThat(maxConcurrent.get()).as("최대 동시 실행 수는 1이어야 함").isEqualTo(1);
        assertThat(counter.get()).as("카운터는 정확히 " + threadCount + "이어야 함").isEqualTo(threadCount);
    }

    @Test
    @DisplayName("Null sessionId로 unlock 시도 - IllegalMonitorStateException 또는 NullPointerException 발생 가능")
    void sessionLock_UnlockNullSessionId_ShouldHandleGracefully() {
        // 이 테스트는 현재 구현의 버그를 확인하는 테스트입니다.
        // null을 전달하면 computeIfAbsent가 NullPointerException을 던지거나
        // 잘못된 lock을 unlock하려고 시도할 수 있습니다.

        // 이상적으로는 null 체크를 해야 합니다.
        assertThatThrownBy(() -> {
            sessionLock.unlockBySessionId(null);
        }).isInstanceOfAny(NullPointerException.class, IllegalMonitorStateException.class);
    }

    @Test
    @DisplayName("Lock을 획득하지 않고 unlock 시도 - IllegalMonitorStateException")
    void sessionLock_UnlockWithoutLock_ShouldThrowException() {
        String sessionId = "test-session";

        assertThatThrownBy(() -> {
            sessionLock.unlockBySessionId(sessionId);
        }).isInstanceOf(IllegalMonitorStateException.class);
    }

    @Test
    @DisplayName("중복 unlock 시도 - IllegalMonitorStateException")
    void sessionLock_DoubleUnlock_ShouldThrowException() {
        String sessionId = "test-session";

        sessionLock.lockBySessionId(sessionId);
        sessionLock.unlockBySessionId(sessionId);

        // 두 번째 unlock은 예외를 던져야 함
        assertThatThrownBy(() -> {
            sessionLock.unlockBySessionId(sessionId);
        }).isInstanceOf(IllegalMonitorStateException.class);
    }

    @RepeatedTest(20)
    @DisplayName("여러 세션의 락 동시 획득 - 각 세션은 독립적이어야 함")
    void sessionLock_MultipleSessions_ShouldBeIndependent() throws InterruptedException {
        int sessionCount = 5;
        int threadsPerSession = 4;
        int totalThreads = sessionCount * threadsPerSession;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(totalThreads);

        AtomicInteger[] counters = new AtomicInteger[sessionCount];
        for (int i = 0; i < sessionCount; i++) {
            counters[i] = new AtomicInteger(0);
        }

        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int s = 0; s < sessionCount; s++) {
            final int sessionNum = s;
            final String sessionId = "session-" + s;

            for (int t = 0; t < threadsPerSession; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        sessionLock.lockBySessionId(sessionId);
                        try {
                            int value = counters[sessionNum].get();
                            Thread.sleep(5);
                            counters[sessionNum].set(value + 1);
                        } finally {
                            sessionLock.unlockBySessionId(sessionId);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                        log.error("Exception in session {}", sessionNum, e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
        }

        startLatch.countDown();
        assertThat(endLatch.await(15, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        if (!exceptions.isEmpty()) {
            log.error("=== Multiple Sessions Test Exceptions ===");
            exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
        }

        assertThat(exceptions).isEmpty();
        for (int i = 0; i < sessionCount; i++) {
            assertThat(counters[i].get()).as("Session " + i + " 카운터").isEqualTo(threadsPerSession);
        }
    }

    @RepeatedTest(20)
    @DisplayName("회원 락 동시 획득 - 순차적으로 실행되어야 함")
    void memberLock_ConcurrentAccess_ShouldBeSequential() throws InterruptedException {
        Long memberId = 1L;
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    memberLock.lockByMemberId(memberId);
                    try {
                        int concurrent = currentConcurrent.incrementAndGet();
                        maxConcurrent.updateAndGet(max -> Math.max(max, concurrent));

                        int value = counter.get();
                        Thread.sleep(10);
                        counter.set(value + 1);

                        currentConcurrent.decrementAndGet();
                    } finally {
                        memberLock.unlockByMemberId(memberId);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Exception in thread {}", threadNum, e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(endLatch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        if (!exceptions.isEmpty()) {
            log.error("=== Member Lock Test Exceptions ===");
            exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
        }

        assertThat(exceptions).isEmpty();
        assertThat(maxConcurrent.get()).as("최대 동시 실행 수는 1이어야 함").isEqualTo(1);
        assertThat(counter.get()).as("카운터는 정확히 " + threadCount + "이어야 함").isEqualTo(threadCount);
    }
}
