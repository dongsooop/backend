package com.dongsoop.dongsoop.notification.dto;

import java.util.List;

public record NotificationOverview(

        List<NotificationList> notificationLists,

        Long unreadCount
) {
}
