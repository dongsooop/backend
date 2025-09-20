package com.dongsoop.dongsoop.notification.dto;

import lombok.Getter;

@Getter
public final class NotificationUnread {

    private final Long memberId;
    private final int unreadCount;

    public NotificationUnread(Long memberId, Long unreadCount) {
        this.memberId = memberId;

        int unread = unreadCount.intValue();

        if (unreadCount == null || unreadCount < 0) {
            unread = 0;
        }

        if (unreadCount > Integer.MAX_VALUE) {
            unread = Integer.MAX_VALUE;
        }

        this.unreadCount = unread;
    }
}
