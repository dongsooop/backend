package com.dongsoop.dongsoop.blinddate.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 과팅(BlindDate) 상태 변경 이벤트를 순서대로 처리하는 단일 스레드 큐.
 * <p>
 * 입장/재접속/세션 시작 판단/퇴장/인원 브로드캐스트 등 참가자·세션 상태를 건드리는 작업은
 * 전부 이 큐를 통해 제출되어 하나의 스레드에서 순서대로 처리된다. 처리를 담당하는 스레드가
 * 항상 하나뿐이므로, 이 상태들에 대해서는 별도의 락이 필요 없다.
 * (BlindDateMatchingLock, BlindDateMemberLock, BlindDateSessionLock 대체)
 */
@Slf4j
@Component
public class BlindDateEventQueue {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 이벤트를 큐에 넣는다. 이미 큐에 있는 다른 이벤트들이 처리된 뒤 순서대로 실행된다.
     */
    public void submit(Runnable event) {
        executor.execute(() -> {
            try {
                event.run();
            } catch (Exception e) {
                log.error("[BlindDate] Event processing failed", e);
            }
        });
    }

    /**
     * 과팅 종료 시 큐 정리
     */
    public void shutdown() {
        executor.shutdownNow();
    }

    /**
     * 지금까지 제출된 이벤트가 모두 처리될 때까지 대기한다.
     * <p>
     * 큐가 단일 스레드로 순서대로(FIFO) 처리되므로, 트리비얼 작업을 제출해 그 작업이 끝날 때까지
     * 기다리면 그 이전에 제출된 모든 이벤트의 처리가 끝났음을 보장할 수 있다. 테스트에서 비동기
     * 처리 완료를 기다릴 때 사용한다.
     */
    public void awaitIdle() {
        try {
            executor.submit(() -> null).get();
        } catch (Exception e) {
            throw new IllegalStateException("[BlindDate] Failed to await queue idle", e);
        }
    }
}
