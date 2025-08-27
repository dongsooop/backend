package com.dongsoop.dongsoop.recruitment.apply.study.notification;

import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import com.dongsoop.dongsoop.recruitment.apply.notification.RecruitmentApplyNotification;
import org.springframework.stereotype.Component;

@Component
public class StudyApplyNotification extends RecruitmentApplyNotification {

    public StudyApplyNotification(MemberDeviceRepositoryCustom memberDeviceRepositoryCustom,
                                  FCMService fcmService,
                                  NotificationService notificationService) {
        super(memberDeviceRepositoryCustom, fcmService, notificationService);
    }

    @Override
    protected NotificationType getApplyNotificationType() {
        return NotificationType.RECRUITMENT_STUDY_APPLY;
    }


    @Override
    protected NotificationType getOutcomeNotificationType() {
        return NotificationType.RECRUITMENT_STUDY_APPLY_RESULT;
    }
}
