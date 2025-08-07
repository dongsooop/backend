package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public interface NotificationService {

    void sendNotificationByDepartment(Department department, Set<NoticeDetails> noticeDetailsSet);
}
