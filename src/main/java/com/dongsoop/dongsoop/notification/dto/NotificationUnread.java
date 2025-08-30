package com.dongsoop.dongsoop.notification.dto;

public record NotificationUnread(

        Long memberId,
        Long unreadCount
) {
}
