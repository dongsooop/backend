package com.dongsoop.dongsoop.eclass.notification;

import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentLink;
import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.FcmSilentType;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EclassNotificationImpl implements EclassNotification {

    private static final Long NON_SAVE_NOTIFICATION_ID = -1L;
    private static final String EXPIRED_TITLE = "이클래스 연동이 만료되었습니다";
    private static final String EXPIRED_BODY = "설정에서 다시 연동해 주세요";

    private final NotificationSaveService notificationSaveService;
    private final NotificationSendService notificationSendService;
    private final NotificationSettingRepository notificationSettingRepository;
    private final FCMService fcmService;

    @Value("${eclass.base-url}")
    private String baseUrl;

    @Value("${eclass.reminder.course-name-max-length}")
    private int courseNameMaxLength;

    @Override
    public void sendReminder(EclassAssignment assignment, int daysBefore) {
        MemberDevice device = assignment.getLink()
                .getDevice();

        String title = EclassReminderMessage.title(assignment.getCourseName(), daysBefore, courseNameMaxLength);
        String body = EclassReminderMessage.body(assignment.getTitle(), assignment.getDueAt());
        String value = EclassAssignmentLink.of(baseUrl, assignment.getCourseModuleId());

        sendToDevice(device, title, body, value);
    }

    @Override
    public void sendDueDateChanged(EclassAssignment assignment) {
        MemberDevice device = assignment.getLink()
                .getDevice();

        String title = EclassReminderMessage.dueDateChangedTitle(assignment.getCourseName(), courseNameMaxLength);
        String body = EclassReminderMessage.body(assignment.getTitle(), assignment.getDueAt());
        String value = EclassAssignmentLink.of(baseUrl, assignment.getCourseModuleId());

        sendToDevice(device, title, body, value);
    }

    @Override
    public void sendRelinkSilent(EclassLink link) {
        String deviceToken = link.getDevice()
                .getDeviceToken();
        if (deviceToken == null || deviceToken.isBlank()) {
            return;
        }

        // 재발급 지시가 실패해도 수집 루프를 멈추지 않는다 — 앱 실행 시 상태 조회로도 복구된다
        try {
            fcmService.sendSilentMessage(deviceToken, FcmSilentType.ECLASS_RELINK);
        } catch (Exception exception) {
            log.warn("failed to send eclass relink silent push. linkId: {}", link.getId(), exception);
        }
    }

    @Override
    public void sendExpiredNotice(EclassLink link) {
        sendToDevice(link.getDevice(), EXPIRED_TITLE, EXPIRED_BODY, "");
    }

    /**
     * 회원 기기면 알림함에 남기고, 비회원 기기면 푸시만 보낸다(공지 알림과 같은 규칙).
     */
    private void sendToDevice(MemberDevice device, String title, String body, String value) {
        String deviceToken = device.getDeviceToken();
        if (deviceToken == null || deviceToken.isBlank() || !isEnabled(device)) {
            return;
        }

        Long notificationId = NON_SAVE_NOTIFICATION_ID;
        Member member = device.getMember();
        if (member != null) {
            MemberNotification saved = notificationSaveService.save(member, title, body,
                    NotificationType.ECLASS_ASSIGNMENT, value);
            notificationId = saved.getId()
                    .getDetails()
                    .getId();
        }

        NotificationSend notificationSend = new NotificationSend(notificationId, title, body,
                NotificationType.ECLASS_ASSIGNMENT, value);
        notificationSendService.send(List.of(deviceToken), notificationSend);
    }

    private boolean isEnabled(MemberDevice device) {
        return notificationSettingRepository
                .findByIdDeviceIdAndIdNotificationType(device.getId(), NotificationType.ECLASS_ASSIGNMENT)
                .map(NotificationSetting::getEnabled)
                .orElse(NotificationType.ECLASS_ASSIGNMENT.getDefaultActiveState());
    }
}
