package com.dongsoop.dongsoop.chat.notification;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import com.dongsoop.dongsoop.notification.service.FCMService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatNotificationImpl implements ChatNotification {

    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final FCMService fcmService;
    private final NotificationRepository notificationRepository;

    public void send(Set<Long> chatroomMemberIdSet, String chatRoomId, String senderName,
                     String message) {
        // 사용자 id를 통해 FCM 토큰을 가져옴
        List<MemberDeviceDto> participantsDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberIds(
                chatroomMemberIdSet);

        List<String> deviceTokens = participantsDevice.stream()
                .map(MemberDeviceDto::deviceToken)
                .toList();

        fcmService.sendNotification(deviceTokens, senderName, message, NotificationType.CHAT, chatRoomId);

        saveMemberNotification(chatRoomId, senderName, message, participantsDevice);
    }

    private void saveMemberNotification(String chatRoomId, String senderName,
                                        String message,
                                        List<MemberDeviceDto> participantsDevice) {
        NotificationDetails details = NotificationDetails.builder()
                .title(senderName)
                .body(message)
                .type(NotificationType.CHAT)
                .value(chatRoomId)
                .build();

        List<MemberNotification> memberNotificationList = participantsDevice.stream()
                .map(memberDeviceDto -> new MemberNotification(details, memberDeviceDto.member()))
                .toList();

        notificationRepository.saveAll(memberNotificationList);
    }
}
