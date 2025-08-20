package com.dongsoop.dongsoop.recruitment.apply.tutoring.notification;

import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.recruitment.apply.notification.RecruitmentApplyNotification;
import org.springframework.stereotype.Component;

@Component
public class TutoringApplyNotification extends RecruitmentApplyNotification {

    public TutoringApplyNotification(MemberDeviceRepositoryCustom memberDeviceRepositoryCustom,
                                     FCMService fcmService) {
        super(memberDeviceRepositoryCustom, fcmService);
    }

    @Override
    protected NotificationType getNotificationType() {
        return NotificationType.RECRUITMENT_TUTORING_APPLY;
    }
}
