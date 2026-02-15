package com.dongsoop.dongsoop.blinddate.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * BlindDate 사용자 구독 이벤트
 */
@Getter
@RequiredArgsConstructor
public class BlindDateUserSubscribedEvent {
    private final String socketId;
    private final Long memberId;
    private final String destination;
}
