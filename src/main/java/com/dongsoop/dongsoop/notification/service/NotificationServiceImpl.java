package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.google.firebase.messaging.FirebaseMessagingException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final FCMService fcmService;

    @Override
    public void sendNotificationByDepartment(Department department, Set<NoticeDetails> noticeDetailsSet) {
        List<String> deviceTokenList = memberDeviceRepositoryCustom.getMemberDeviceByDepartment(
                department);

        noticeDetailsSet.forEach(noticeDetails -> {
            try {
                fcmService.sendNotification(deviceTokenList, noticeDetails.getTitle(), noticeDetails.getWriter());
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send notification for department: {}, notice: {}",
                        department.getId().getId(), noticeDetails.getTitle(), e);
                throw new RuntimeException(e);
            }
        });
    }
}
