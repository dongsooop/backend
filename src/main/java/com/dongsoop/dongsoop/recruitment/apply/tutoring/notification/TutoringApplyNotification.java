package com.dongsoop.dongsoop.recruitment.apply.tutoring.notification;

import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.recruitment.apply.notification.RecruitmentApplyNotification;
import org.springframework.stereotype.Component;

@Component
public class TutoringApplyNotification extends RecruitmentApplyNotification {

    public TutoringApplyNotification(NotificationSaveService notificationSaveService,
                                     NotificationSendService notificationSendService,
                                     MemberRepository memberRepository) {
        super(notificationSaveService, notificationSendService, memberRepository);
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
