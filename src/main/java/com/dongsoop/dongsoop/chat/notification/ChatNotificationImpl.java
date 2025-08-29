package com.dongsoop.dongsoop.chat.notification;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
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
 
    public void send(Set<Long> chatroomMemberIdSet, String chatRoomId, String senderName,
                     String message) {
        // 사용자 id를 통해 FCM 토큰을 가져옴
        List<MemberDeviceDto> participantsDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberIds(
                chatroomMemberIdSet);

        List<String> deviceTokens = participantsDevice.stream()
                .map(MemberDeviceDto::deviceToken)
                .toList();

        NotificationSend notificationSend = new NotificationSend(null, senderName, message, NotificationType.CHAT,
                chatRoomId);

        fcmService.sendNotification(deviceTokens, notificationSend);
    }
}
