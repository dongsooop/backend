package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notice.entity.Notice;
import java.util.Set;

public interface NotificationService {

    void sendNotificationByDepartment(Set<Notice> noticeDetailSet);

    void sendNotificationForChat(Set<Long> chatroomMemberIdSet, String senderName, String message);
}
