package com.dongsoop.dongsoop.blinddate.support;

import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.PriorityQueue;

/** 외부 타이머 경계를 대체한다. 테스트는 시간을 입력하고 도메인 결과를 관찰한다. */
public class ManualBlindDateTaskScheduler extends BlindDateTaskScheduler {
    private final LocalDateTime origin = LocalDateTime.now();
    private final PriorityQueue<Task> tasks = new PriorityQueue<>(
            Comparator.comparingLong(Task::due).thenComparingLong(Task::sequence));
    private long now;
    private long sequence;
    private Long rejectedDelay;
    private boolean rejectAbsolute;

    @Override
    public void execute(Runnable task) {
        task.run();
    }

    @Override
    public void schedule(Runnable task, long delay) {
        if (rejectedDelay != null && rejectedDelay == delay) {
            throw new IllegalStateException("Timer unavailable");
        }
        tasks.add(new Task(now + delay, sequence++, task));
    }

    @Override
    public void schedule(Runnable task, LocalDateTime endTime) {
        if (rejectAbsolute) {
            throw new IllegalStateException("Timer unavailable");
        }
        tasks.add(new Task(Duration.between(origin, endTime).toMillis(), sequence++, task));
    }

    @Override
    public void cleanupAllSessions() {
        tasks.clear();
    }

    public void advanceBy(long millis) {
        long target = now + millis;
        while (!tasks.isEmpty() && tasks.peek().due() <= target) {
            Task next = tasks.remove();
            now = next.due();
            next.action().run();
        }
        now = target;
    }

    public void rejectDelay(long delay) {
        rejectedDelay = delay;
    }

    public void rejectAbsoluteSchedules() {
        rejectAbsolute = true;
    }

    private record Task(long due, long sequence, Runnable action) {}
}
