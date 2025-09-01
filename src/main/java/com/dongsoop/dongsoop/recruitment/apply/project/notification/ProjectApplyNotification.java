package com.dongsoop.dongsoop.recruitment.apply.project.notification;

import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.recruitment.apply.notification.RecruitmentApplyNotification;
import org.springframework.stereotype.Component;

@Component
public class ProjectApplyNotification extends RecruitmentApplyNotification {

    public ProjectApplyNotification(NotificationSaveService notificationSaveService,
                                    NotificationSendService notificationSendService,
                                    MemberRepository memberRepository) {
        super(notificationSaveService, notificationSendService, memberRepository);
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
