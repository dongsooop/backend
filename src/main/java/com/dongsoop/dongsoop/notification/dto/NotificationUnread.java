package com.dongsoop.dongsoop.notification.dto;

import lombok.Getter;

@Getter
public final class NotificationUnread {

    private final Long memberId;
    private final int unreadCount;

    public NotificationUnread(Long memberId, Long unreadCount) {
        this.memberId = memberId;

        if (unreadCount == null || unreadCount < 0) {
            this.unreadCount = 0;
        } else if (unreadCount > Integer.MAX_VALUE) {
            this.unreadCount = Integer.MAX_VALUE;
        } else {
            this.unreadCount = unreadCount.intValue();
        }
    }
}
