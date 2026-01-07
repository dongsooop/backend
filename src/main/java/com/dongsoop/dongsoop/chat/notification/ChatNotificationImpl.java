package com.dongsoop.dongsoop.chat.notification;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatNotificationImpl implements ChatNotification {

    private final static Long NON_SAVE_NOTIFICATION_ID = -1L;

    private final MemberDeviceRepository memberDeviceRepository;
    private final NotificationSendService notificationSendService;

    @Async
    public void send(Set<Long> chatroomMemberIdSet, String chatRoomId, String senderName,
                     String message) {
        // 사용자 id를 통해 FCM 토큰을 가져옴
        MemberDeviceFindCondition condition = new MemberDeviceFindCondition(
                chatroomMemberIdSet, NotificationType.CHAT);
        List<MemberDeviceDto> participantsDevice = memberDeviceRepository.findDevicesWithNotification(condition);

        List<String> deviceTokens = participantsDevice.stream()
                .map(MemberDeviceDto::deviceToken)
                .toList();

        NotificationSend notificationSend = new NotificationSend(NON_SAVE_NOTIFICATION_ID, senderName, message,
                NotificationType.CHAT, chatRoomId);

        notificationSendService.send(deviceTokens, notificationSend);
    }
}
