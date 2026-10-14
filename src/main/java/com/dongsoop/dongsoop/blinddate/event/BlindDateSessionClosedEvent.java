package com.dongsoop.dongsoop.blinddate.event;

import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import java.util.List;

/**
 * 세션이 종료·초기화로 메모리에서 지워질 때 발행한다. 메시지 기록은 이 이벤트 이후 어디에도 남지 않는다.
 */
public record BlindDateSessionClosedEvent(String sessionId, List<BlindDateMessage> messages) {
}
