package com.dongsoop.dongsoop.feedback.notification;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedbackNotificationImpl implements FeedbackNotification {

    private static final String FEEDBACK_BODY = "새로운 피드백이 등록되었습니다! 내용을 확인해주세요!";

    private final MemberRepository memberRepository;
    private final NotificationSaveService notificationSaveService;
    private final NotificationSendService notificationSendService;

    @Override
    public void send(String improvementSuggestions, Long feedbackId) {
        List<Member> memberList = memberRepository.findByRoleTypeWithDevice(RoleType.ADMIN);
        if (memberList.isEmpty()) {
            return;
        }

        List<MemberNotification> memberNotificationList = notificationSaveService.saveAll(memberList,
                improvementSuggestions,
                FEEDBACK_BODY, NotificationType.FEEDBACK, String.valueOf(feedbackId));

        notificationSendService.sendAll(memberNotificationList, NotificationType.FEEDBACK);
    }
}
