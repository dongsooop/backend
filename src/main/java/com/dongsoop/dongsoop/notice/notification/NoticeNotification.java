package com.dongsoop.dongsoop.notice.notification;

import com.dongsoop.dongsoop.notice.entity.Notice;
import java.util.Set;

public interface NoticeNotification {

    void send(Set<Notice> noticeDetailSet);
}
