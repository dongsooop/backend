package com.dongsoop.dongsoop.recruitment.apply.tutoring.notification;

import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import com.dongsoop.dongsoop.recruitment.apply.notification.RecruitmentApplyNotification;
import org.springframework.stereotype.Component;

@Component
public class TutoringApplyNotification extends RecruitmentApplyNotification {

    public TutoringApplyNotification(MemberDeviceRepositoryCustom memberDeviceRepositoryCustom,
                                     NotificationService notificationService) {
        super(memberDeviceRepositoryCustom, notificationService);
    }

    @Override
    protected NotificationType getApplyNotificationType() {
        return NotificationType.RECRUITMENT_TUTORING_APPLY;
    }

    @Override
    protected NotificationType getOutcomeNotificationType() {
        return NotificationType.RECRUITMENT_TUTORING_APPLY_RESULT;
    }
}
