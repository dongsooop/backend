package com.dongsoop.dongsoop.notification.service;

import java.util.Collection;
import java.util.Map;

/**
 * 알림 뱃지(회원별 읽지 않은 알림 수) 계산과 갱신 전담.
 *
 * <p>뱃지 계산이 발송·읽음 처리 곳곳에 흩어져 있으면 한쪽만 고쳐 값이 어긋나기 쉬워 한곳으로 모은다.
 */
public interface NotificationBadgeService {

    /**
     * 여러 회원의 뱃지를 한 번에 계산한다. 발송 시 알림 payload 에 실을 값이다.
     */
    Map<Long, Integer> resolveByMemberIds(Collection<Long> memberIds);

    /**
     * 한 회원의 뱃지를 계산한다.
     */
    int resolveByMemberId(Long memberId);

    /**
     * 한 회원의 뱃지를 계산해 그 회원의 기기에 갱신 신호를 보낸다.
     * 읽음 처리처럼 알림 발송 없이 뱃지만 바뀌는 경우에 쓴다.
     */
    void pushBadge(Long memberId);
}
