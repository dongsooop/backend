package com.dongsoop.dongsoop.chat.notification;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatNotificationImpl implements ChatNotification {

    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final FCMService fcmService;
    private final NotificationService notificationService;

    public void send(Set<Long> chatroomMemberIdSet, String chatRoomId, String senderName,
                     String message) {
        // 사용자 id를 통해 FCM 토큰을 가져옴
        List<MemberDeviceDto> participantsDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberIds(
                chatroomMemberIdSet);

        List<String> deviceTokens = participantsDevice.stream()
                .map(MemberDeviceDto::deviceToken)
                .toList();

        fcmService.sendNotification(deviceTokens, senderName, message, NotificationType.CHAT, chatRoomId);

        notificationService.save(participantsDevice, senderName, message, NotificationType.CHAT, chatRoomId);
    }
}
