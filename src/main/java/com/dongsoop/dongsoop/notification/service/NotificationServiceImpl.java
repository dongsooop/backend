package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final FCMService fcmService;

    @Value("${university.domain}")
    private String universityDomain;

    @Override
    @Transactional(readOnly = true)
    public void sendNotificationByDepartment(Set<Notice> noticeDetailSet) {
        if (noticeDetailSet.isEmpty()) {
            return;
        }
        
        List<Message> messageList = convertNoticeToMessage(noticeDetailSet);
        fcmService.sendMessages(messageList);
    }

    /**
     * 공지사항을 Message로 변환
     *
     * @param noticeSet { 학과: 공지사항 세부 } 구조인 Notice Set
     * @return Message 리스트
     */
    private List<Message> convertNoticeToMessage(Set<Notice> noticeSet) {
        return noticeSet.stream()
                .flatMap((notice) -> {
                    Department department = notice.getDepartment();
                    String title = notice.getNoticeDetails().getTitle();
                    String writer = notice.getNoticeDetails().getWriter();
                    List<String> deviceTokens = getDeviceTokensByDepartment(department);
                    if (deviceTokens.isEmpty()) {
                        return Stream.empty();
                    }

                    String noticeUrl = universityDomain + notice.getNoticeDetails().getLink();

                    ApnsConfig apnsConfig = fcmService.getApnsConfig(title, writer, NotificationType.NOTICE, noticeUrl);
                    Notification notification = Notification.builder()
                            .setTitle(title)
                            .setBody(writer)
                            .build();

                    return deviceTokens.stream()
                            .map(token -> Message.builder()
                                    .setToken(token)
                                    .setApnsConfig(apnsConfig)
                                    .setNotification(notification)
                                    .build());
                })
                .toList();
    }

    private List<String> getDeviceTokensByDepartment(Department department) {
        if (department.getId().isAllDepartment()) {
            return memberDeviceRepositoryCustom.getAllMemberDevice();
        }

        return memberDeviceRepositoryCustom.getMemberDeviceByDepartment(department);
    }

    @Override
    public void sendNotificationForChat(Set<Long> chatroomMemberIdSet, String chatRoomId, String senderName,
                                        String message) {
        // 사용자 id를 통해 FCM 토큰을 가져옴
        List<String> participantsDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberIds(
                chatroomMemberIdSet);

        fcmService.sendNotification(participantsDevice, senderName, message, NotificationType.CHAT, chatRoomId);
    }
}
