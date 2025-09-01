package com.dongsoop.dongsoop.recruitment.apply.study.notification;

import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.recruitment.apply.notification.RecruitmentApplyNotification;
import org.springframework.stereotype.Component;

@Component
public class StudyApplyNotification extends RecruitmentApplyNotification {

    public StudyApplyNotification(NotificationSaveService notificationSaveService,
                                  NotificationSendService notificationSendService,
                                  MemberRepository memberRepository) {
        super(notificationSaveService, notificationSendService, memberRepository);
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
