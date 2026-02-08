package com.dongsoop.dongsoop.blinddate.scheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private final List<ScheduledFuture<?>> futures = new CopyOnWriteArrayList<>();
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
        }, endTime.atZone(ZoneId.systemDefault()).toInstant());

        // 작업 등록
        futures.add(schedule);

        log.info("BlindDate end cleanup scheduled for {}", endTime);
    }

    /**
     * 모든 세션 정리
     */
    public void cleanupAllSessions() {
        futures.forEach((future) -> future.cancel(true));
        log.info("All sessions cleaned up");
    }
}
