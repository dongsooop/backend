package com.dongsoop.dongsoop.blinddate.executor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 과팅 참여 이벤트와 세션별 선택·마감 이벤트의 실행 순서를 관리한다. */
@Slf4j
@Component
public class BlindDateEventQueue {

    private static final int SESSION_WORKER_COUNT = 5;

    private final ExecutorService participantExecutor;
    private final ExecutorService sessionWorkers;
    private final Map<String, SerialExecutor> sessionExecutors = new HashMap<>();
    private final Set<String> openChoiceSessions = new HashSet<>();
    private int pendingCleanups;

    public BlindDateEventQueue() {
        this(SESSION_WORKER_COUNT);
    }

    BlindDateEventQueue(int sessionWorkerCount) {
        participantExecutor = Executors.newSingleThreadExecutor();
        sessionWorkers = Executors.newFixedThreadPool(sessionWorkerCount);
    }

    /** 시간 제한 없이 전원 응답을 기다린다. */
    public synchronized void openChoices(String sessionId) {
        openChoices(sessionId, () -> true);
    }

    /** 초기화와 접수 개설을 같은 잠금에서 검사한다. initialize는 저장소 상태만 변경한다. */
    public synchronized boolean openChoices(String sessionId, BooleanSupplier initialize) {
        if (pendingCleanups > 0 || !initialize.getAsBoolean()) {
            return false;
        }
        openChoiceSessions.add(sessionId);
        return true;
    }

    /** 접수 확인과 세션 큐 삽입을 마감 처리와 같은 잠금으로 묶는다. */
    public synchronized void submitChoice(String sessionId, Runnable choice) {
        if (!openChoiceSessions.contains(sessionId)) {
            log.info("[BlindDate] Ignore choice outside choice period: sessionId={}", sessionId);
            return;
        }

        try {
            sessionExecutor(sessionId).execute(wrap(choice));
        } catch (Exception e) {
            log.error("[BlindDate] Failed to submit choice: sessionId={}", sessionId, e);
        }
    }

    /** 접수를 먼저 닫고, 같은 세션에서 이미 접수한 선택들 뒤에 결과 확정 작업을 넣는다. */
    public synchronized void closeChoices(String sessionId, Runnable finalizeSession) {
        if (!openChoiceSessions.remove(sessionId)) {
            return;
        }

        SerialExecutor sessionExecutor = sessionExecutor(sessionId);
        try {
            sessionExecutor.execute(wrap(() -> {
                try {
                    finalizeSession.run();
                } finally {
                    removeSessionExecutor(sessionId, sessionExecutor);
                }
            }));
        } catch (Exception e) {
            removeSessionExecutor(sessionId, sessionExecutor);
            log.error("[BlindDate] Failed to submit finalization: sessionId={}", sessionId, e);
        }
    }

    /** 입장·재접속·퇴장·운영 상태처럼 세션 배정 전후의 공용 상태 변경을 순서대로 처리한다. */
    public void submit(Runnable event) {
        try {
            participantExecutor.execute(wrap(event));
        } catch (Exception e) {
            log.error("[BlindDate] Failed to submit participant event", e);
        }
    }

    /** 새 선택 접수를 닫고, 이미 제출된 세션 작업이 끝난 뒤 전체 상태 정리를 실행한다. */
    public synchronized void submitCleanup(Runnable cleanup) {
        pendingCleanups++;
        openChoiceSessions.clear();

        Map<String, SerialExecutor> executorsToClean = new HashMap<>(sessionExecutors);
        List<CompletableFuture<Void>> sessionBarriers = executorsToClean.values().stream()
                .map(SerialExecutor::barrier)
                .toList();

        try {
            participantExecutor.execute(wrap(() -> {
                try {
                    sessionBarriers.forEach(this::await);
                    cleanup.run();
                } finally {
                    completeCleanup(executorsToClean);
                }
            }));
        } catch (Exception e) {
            pendingCleanups--;
            log.error("[BlindDate] Failed to submit cleanup", e);
        }
    }

    private synchronized void completeCleanup(Map<String, SerialExecutor> executorsToClean) {
        executorsToClean.forEach(this::removeSessionExecutor);
        pendingCleanups--;
    }

    private Runnable wrap(Runnable event) {
        return () -> {
            try {
                event.run();
            } catch (Exception e) {
                log.error("[BlindDate] Event processing failed", e);
            }
        };
    }

    private synchronized SerialExecutor sessionExecutor(String sessionId) {
        return sessionExecutors.computeIfAbsent(sessionId, ignored -> new SerialExecutor(sessionWorkers));
    }

    private synchronized void removeSessionExecutor(String sessionId, SerialExecutor executor) {
        sessionExecutors.remove(sessionId, executor);
    }

    synchronized int activeSessionQueueCount() {
        return sessionExecutors.size();
    }

    /** 애플리케이션 빈 종료 시 참여 executor와 세션 worker를 함께 정리한다. */
    public void shutdown() {
        participantExecutor.shutdownNow();
        sessionWorkers.shutdownNow();
    }

    /** 호출 시점까지 참여 큐와 각 세션 큐에 제출된 작업이 모두 끝날 때까지 기다린다. */
    public void awaitIdle() {
        await(participantExecutor);

        List<CompletableFuture<Void>> barriers;
        synchronized (this) {
            barriers = new ArrayList<>(sessionExecutors.size());
            sessionExecutors.values().forEach(executor -> barriers.add(executor.idle()));
        }
        barriers.forEach(this::await);
    }

    private void await(ExecutorService executor) {
        try {
            executor.submit(() -> null).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("[BlindDate] Interrupted while awaiting queue idle", e);
        } catch (Exception e) {
            throw new IllegalStateException("[BlindDate] Failed to await queue idle", e);
        }
    }

    private void await(CompletableFuture<Void> barrier) {
        try {
            barrier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("[BlindDate] Interrupted while awaiting session queue idle", e);
        } catch (Exception e) {
            throw new IllegalStateException("[BlindDate] Failed to await session queue idle", e);
        }
    }

    /** 공용 worker 위에서 자신에게 제출된 작업만 한 번에 하나씩 실행한다. */
    private static final class SerialExecutor {

        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private final Executor backend;
        private Runnable active;
        private final List<CompletableFuture<Void>> idleWaiters = new ArrayList<>();

        private SerialExecutor(Executor backend) {
            this.backend = backend;
        }

        private synchronized void execute(Runnable task) {
            tasks.offer(() -> {
                try {
                    task.run();
                } finally {
                    scheduleNext();
                }
            });
            if (active == null) {
                scheduleNext();
            }
        }

        private CompletableFuture<Void> barrier() {
            CompletableFuture<Void> barrier = new CompletableFuture<>();
            execute(() -> barrier.complete(null));
            return barrier;
        }

        private synchronized CompletableFuture<Void> idle() {
            if (active == null) {
                return CompletableFuture.completedFuture(null);
            }
            CompletableFuture<Void> waiter = new CompletableFuture<>();
            idleWaiters.add(waiter);
            return waiter;
        }

        private synchronized void scheduleNext() {
            active = tasks.poll();
            if (active == null) {
                idleWaiters.forEach(waiter -> waiter.complete(null));
                idleWaiters.clear();
            } else {
                try {
                    backend.execute(active);
                } catch (RuntimeException e) {
                    active = null;
                    tasks.clear();
                    throw e;
                }
            }
        }
    }
}
