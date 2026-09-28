package com.dongsoop.dongsoop.blinddate.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * 과팅 세션 동안 신고 증거로 쓸 메시지 보관소. 세션 정보와 함께 사라진다.
 */
public class BlindDateMessageLog {

    // 세션이 짧아 실제로는 닿지 않는 상한. 메모리 폭주만 막는다
    private static final int MAX_MESSAGES = 1000;

    private final Deque<BlindDateMessage> messages = new ArrayDeque<>();

    public synchronized void add(BlindDateMessage message) {
        messages.addLast(message);
        if (messages.size() > MAX_MESSAGES) {
            messages.removeFirst();
        }
    }

    public synchronized Optional<BlindDateMessage> find(String messageId) {
        return messages.stream()
                .filter(message -> message.messageId().equals(messageId))
                .findFirst();
    }

    public synchronized List<BlindDateMessage> findBefore(String messageId, int limit) {
        List<BlindDateMessage> before = new ArrayList<>();
        for (BlindDateMessage message : messages) {
            if (message.messageId().equals(messageId)) {
                break;
            }
            before.add(message);
        }

        return List.copyOf(before.subList(Math.max(0, before.size() - limit), before.size()));
    }
}
