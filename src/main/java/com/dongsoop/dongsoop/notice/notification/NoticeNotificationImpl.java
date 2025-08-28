package com.dongsoop.dongsoop.notice.notification;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoticeNotificationImpl implements NoticeNotification {

    private final FCMService fcmService;
    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final NotificationService notificationService;

    @Value("${university.domain}")
    private String universityDomain;

    @Async
    public void send(Set<Notice> noticeDetailSet) {
        if (noticeDetailSet == null || noticeDetailSet.isEmpty()) {
            return;
        }

        saveMemberNotification(noticeDetailSet);
        List<Message> messageList = convertNoticeToMessage(noticeDetailSet);
        fcmService.sendMessages(messageList);
    }

    /**
     * 공지사항 알림을 DB에 저장
     *
     * @param noticeSet { 학과: 공지사항 세부 } 구조인 Notice Set
     */
    private void saveMemberNotification(Set<Notice> noticeSet) {
        noticeSet.forEach((notice) -> {
            Department department = notice.getDepartment();
            String departmentName = department.getId().getName();

            String title = generateTitle(departmentName);
            String body = notice.getNoticeDetails().getTitle();
            List<MemberDeviceDto> deviceTokens = getDeviceTokensByDepartment(department);

            String noticeLink = universityDomain + notice.getNoticeDetails().getLink();
            notificationService.save(deviceTokens, title, body, NotificationType.NOTICE, noticeLink);
        });
    }

    /**
     * 공지사항을 Message로 변환
     *
     * @param noticeSet { 학과: 공지사항 세부 } 구조인 Notice Set
     * @return Message 리스트
     */
    private List<Message> convertNoticeToMessage(Set<Notice> noticeSet) {
        return noticeSet.stream()
                .flatMap(this::convertEachNoticeToMessage)
                .toList();
    }

    private Stream<Message> convertEachNoticeToMessage(Notice notice) {
        Department department = notice.getDepartment();
        String departmentName = department.getId().getName();

        String title = generateTitle(departmentName);
        String body = notice.getNoticeDetails().getTitle();
        List<MemberDeviceDto> deviceTokens = getDeviceTokensByDepartment(department);
        if (deviceTokens.isEmpty()) {
            return Stream.empty();
        }

        String noticeLink = universityDomain + notice.getNoticeDetails().getLink();
        return generateMessages(title, body, deviceTokens, noticeLink);
    }

    private Stream<Message> generateMessages(String title, String body, List<MemberDeviceDto> deviceTokens,
                                             String noticeId) {
        ApnsConfig apnsConfig = fcmService.getApnsConfig(title, body, NotificationType.NOTICE, noticeId);
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        return deviceTokens.stream()
                .map(token -> Message.builder()
                        .setToken(token.deviceToken())
                        .setApnsConfig(apnsConfig)
                        .setNotification(notification)
                        .build());
    }

    private String generateTitle(String departmentName) {
        return String.format("[%s] 공지 알림", departmentName);
    }

    private List<MemberDeviceDto> getDeviceTokensByDepartment(Department department) {
        if (department.getId().isAllDepartment()) {
            return memberDeviceRepositoryCustom.getAllMemberDevice();
        }

        return memberDeviceRepositoryCustom.getMemberDeviceByDepartment(department);
    }
}
