package com.dongsoop.dongsoop.blinddate.scheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BlindDateTaskScheduler {

    private static final int THREAD_POOL_SIZE = 1;

    // 종료된 세션
    private final Set<String> endedSessions = ConcurrentHashMap.newKeySet();
    private final List<ScheduledFuture<?>> futures = new LinkedList<>();
    private final TaskScheduler taskScheduler = new ConcurrentTaskScheduler(
            Executors.newScheduledThreadPool(THREAD_POOL_SIZE));

    /**
     * 즉시 실행 (비동기)
     */
    public void execute(Runnable task) {
        new Thread(task).start();
    }

    /**
     * 지연 후 실행
     */
    public void schedule(Runnable task, long delay) {
        ScheduledFuture<?> schedule = this.taskScheduler.schedule(task, Instant.now().plusMillis(delay));

        // 작업 등록
        futures.add(schedule);
    }

    /**
     * 과팅 종료 시점 스케줄링
     */
    public void scheduleBlindDateEnd(LocalDateTime endTime, Runnable cleanupTask) {
        ScheduledFuture<?> schedule = taskScheduler.schedule(() -> {
            try {
                log.info("[BlindDate] Executing BlindDate end cleanup at {}", LocalDateTime.now());
                cleanupTask.run();
            } catch (Exception e) {
                log.error("[BlindDate] Error during BlindDate cleanup", e);
            }
        }, Instant.ofEpochMilli(endTime.toEpochSecond(ZoneOffset.UTC)));

        // 작업 등록
        futures.add(schedule);

        log.info("BlindDate end cleanup scheduled for {}", endTime);
    }

    /**
     * 세션 정리 (종료 시)
     */
    public void cleanupSession(String sessionId) {
        // 종료된 세션 set에 저장해 스케줄링 시 검증 선행
        endedSessions.add(sessionId);

        log.debug("[BlindDate] Cleaned up session: {}", sessionId);
    }

    /**
     * 모든 세션 정리
     */
    public void cleanupAllSessions() {
        futures.forEach((future) -> future.cancel(true));
        log.info("All sessions cleaned up");
    }
}
