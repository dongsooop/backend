package com.dongsoop.dongsoop.eclass.notification;

import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentResponse;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
    private static final DateTimeFormatter DUE_FORMATTER =
            DateTimeFormatter.ofPattern("M월 d일 (E) HH:mm", Locale.KOREAN);

    private final NotificationSaveService notificationSaveService;
    private final NotificationSendService notificationSendService;
    private final NotificationSettingRepository notificationSettingRepository;
    private final FCMService fcmService;

    @Value("${eclass.base-url}")
    private String baseUrl;

    @Value("${eclass.reminder.course-name-max-length}")
    private int courseNameMaxLength;

    @Override
    public boolean sendReminder(EclassAssignment assignment, int daysBefore) {
        String courseName = shorten(assignment.getCourseName());
        String title = daysBefore == 0
                ? String.format("[%s] 과제 오늘 마감입니다", courseName)
                : String.format("[%s] 과제 %d일 전입니다", courseName, daysBefore);

        return sendAssignment(assignment.getLink().getDevice(), title, assignment);
    }

    @Override
    public void sendDueDateChanged(EclassLink link, EclassAssignment assignment) {
        // 수집은 트랜잭션 밖에서 돌기 때문에 assignment.getLink()는 초기화되지 않은 프록시다.
        // 호출자가 이미 들고 있는 링크를 그대로 받는다
        String title = String.format("[%s] 과제 마감이 앞당겨졌어요", shorten(assignment.getCourseName()));

        sendAssignment(link.getDevice(), title, assignment);
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

    /**
     * 재연동 유도는 과제 알림 설정과 무관하게 보낸다 — 알림을 꺼둔 사용자도 연동이 끊긴 것은 알아야 한다.
     */
    @Override
    public void sendExpiredNotice(EclassLink link) {
        push(link.getDevice(), EXPIRED_TITLE, EXPIRED_BODY, "");
    }

    /**
     * 마감 시각은 이클래스 화면과 같은 값을 그대로 보여준다 — "전날 밤까지"처럼 바꿔 쓰면 혼란을 준다.
     */
    private boolean sendAssignment(MemberDevice device, String title, EclassAssignment assignment) {
        String body = String.format("%s · 마감 %s", assignment.getTitle(), DUE_FORMATTER.format(assignment.getDueAt()));
        String link = EclassAssignmentResponse.link(baseUrl, assignment.getCourseModuleId());

        return sendToDevice(device, title, body, link);
    }

    private String shorten(String courseName) {
        if (courseName.length() <= courseNameMaxLength) {
            return courseName;
        }

        return courseName.substring(0, courseNameMaxLength) + "…";
    }

    /**
     * 회원 기기면 알림함에 남기고, 비회원 기기면 푸시만 보낸다(공지 알림과 같은 규칙).
     */
    private boolean sendToDevice(MemberDevice device, String title, String body, String value) {
        return isEnabled(device) && push(device, title, body, value);
    }

    private boolean push(MemberDevice device, String title, String body, String value) {
        String deviceToken = device.getDeviceToken();
        if (deviceToken == null || deviceToken.isBlank()) {
            return false;
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

        return true;
    }

    private boolean isEnabled(MemberDevice device) {
        return notificationSettingRepository
                .findByIdDeviceIdAndIdNotificationType(device.getId(), NotificationType.ECLASS_ASSIGNMENT)
                .map(NotificationSetting::getEnabled)
                .orElse(NotificationType.ECLASS_ASSIGNMENT.getDefaultActiveState());
    }
}
