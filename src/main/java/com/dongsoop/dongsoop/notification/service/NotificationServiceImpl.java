package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final FCMService fcmService;

    @Override
    public void sendNotificationForChat(Set<Long> chatroomMemberIdSet, String chatRoomId, String senderName,
                                        String message) {
        // 사용자 id를 통해 FCM 토큰을 가져옴
        List<String> participantsDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberIds(
                chatroomMemberIdSet);

        fcmService.sendNotification(participantsDevice, senderName, message, NotificationType.CHAT, chatRoomId);
    }
}
