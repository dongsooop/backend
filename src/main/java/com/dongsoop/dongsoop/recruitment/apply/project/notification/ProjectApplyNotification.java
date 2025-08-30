package com.dongsoop.dongsoop.recruitment.apply.project.notification;

import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import com.dongsoop.dongsoop.recruitment.apply.notification.RecruitmentApplyNotification;
import org.springframework.stereotype.Component;

@Component
public class ProjectApplyNotification extends RecruitmentApplyNotification {

    public ProjectApplyNotification(MemberDeviceRepository memberDeviceRepository,
                                    NotificationService notificationService) {
        super(memberDeviceRepository, notificationService);
    }

    @Override
    protected NotificationType getApplyNotificationType() {
        return NotificationType.RECRUITMENT_PROJECT_APPLY;
    }

    @Override
    protected NotificationType getOutcomeNotificationType() {
        return NotificationType.RECRUITMENT_PROJECT_APPLY_RESULT;
    }
}
