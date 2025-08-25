package com.dongsoop.dongsoop.recruitment.apply.study.notification;

import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.recruitment.apply.notification.RecruitmentApplyNotification;
import org.springframework.stereotype.Component;

@Component
public class StudyApplyNotification extends RecruitmentApplyNotification {

    public StudyApplyNotification(MemberDeviceRepositoryCustom memberDeviceRepositoryCustom,
                                  FCMService fcmService) {
        super(memberDeviceRepositoryCustom, fcmService);
    }

    @Override
    protected NotificationType getNotificationType() {
        return NotificationType.RECRUITMENT_STUDY_APPLY;
    }
}
