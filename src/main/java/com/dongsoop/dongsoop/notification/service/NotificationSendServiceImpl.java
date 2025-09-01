package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.MulticastMessageWithTokens;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.dto.NotificationUnread;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSendServiceImpl implements NotificationSendService {

    private final MemberDeviceService memberDeviceService;
    private final NotificationRepository notificationRepository;
    private final FCMService fcmService;

    /**
     * 알림 전송
     *
     * @param memberNotificationList 저장된 알림 리스트
     */
    @Override
    @Transactional(readOnly = true)
    public void sendAll(List<MemberNotification> memberNotificationList) {
        // 알림을 보낼 대상 회원들
        List<Long> memberIds = memberNotificationList.stream()
                .map(notification -> notification.getId().getMember().getId())
                .distinct()
                .toList();

        // 저장된 알림 -> Map 변환 (key: 알림 상세, value: 알림 대상 회원 ID 리스트)
        Map<NotificationDetails, List<Long>> memberByNotification = listToMap(
                memberNotificationList);

        // 발송 전체 대상의 디바이스 토큰
        Map<Long, List<String>> memberIdDevices = memberDeviceService.getDeviceByMember(memberIds);

        // 발송 전체 대상의 회원별 읽지 않은 알림 개수
        List<NotificationUnread> unreadCount = notificationRepository.findUnreadCountByMemberIds(memberIds);
        Map<Long, Long> unreadCountByMember = unreadCount.stream()
                .collect(Collectors.toMap(NotificationUnread::memberId, NotificationUnread::unreadCount));

        memberByNotification.entrySet().stream()
                .flatMap(map ->
                        toMulticastMessage(map.getKey(), map.getValue(), memberIdDevices, unreadCountByMember))
                .forEach(message -> fcmService.sendMessages(message.messages(), message.tokens()));
    }

    /**
     * MulticastMessage 변환
     *
     * @param details      공지 세부 정보
     * @param memberIdList 공지 대상 회원 ID 리스트
     * @return MulticastMessage 변환한 메시지
     */
    private Stream<MulticastMessageWithTokens> toMulticastMessage(NotificationDetails details, List<Long> memberIdList,
                                                                  Map<Long, List<String>> memberIdDevices,
                                                                  Map<Long, Long> unreadCountByMember) {
        // 공지별 회원 알림 전송
        Long notificationId = details.getId();
        String title = details.getTitle();
        String body = details.getBody();
        NotificationType type = details.getType();
        String value = details.getValue();

        // 알림 객체 생성
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        ApnsConfig.Builder apnsConfigBuilder = ApnsConfig.builder()
                .putCustomData("type", type)
                .putCustomData("value", value)
                .putCustomData("id", String.valueOf(notificationId));

        // 회원별 MulticastMessage 생성
        return memberIdList.stream().map(memberId -> {
            Long unreadCount = unreadCountByMember.getOrDefault(memberId, 0L);
            List<String> deviceList = memberIdDevices.getOrDefault(memberId, Collections.emptyList());

            return generateMulticastMessage(title, body, deviceList, notification, unreadCount,
                    apnsConfigBuilder);
        });
    }

    private Map<NotificationDetails, List<Long>> listToMap(List<MemberNotification> memberNotificationList) {
        return memberNotificationList.stream().collect(Collectors.groupingBy(
                notification -> notification.getId().getDetails(),
                Collectors.mapping(
                        notification -> notification.getId().getMember().getId(),
                        Collectors.toList()
                )));
    }

    @Override
    public void send(List<String> deviceTokenList, NotificationSend notificationSend) {
        fcmService.sendNotification(deviceTokenList, notificationSend, null);
    }

    /**
     * MulticastMessage 생성
     *
     * @param title             알림 제목
     * @param body              알림 내용
     * @param deviceList        디바이스 토큰 리스트
     * @param notification      알림 객체
     * @param apnsConfigBuilder APNS 설정 빌더
     * @return MulticastMessage 생성한 메시지
     */
    private MulticastMessageWithTokens generateMulticastMessage(String title, String body,
                                                                List<String> deviceList,
                                                                Notification notification,
                                                                long unreadCount,
                                                                ApnsConfig.Builder apnsConfigBuilder) {
        // APNS 생성
        Aps aps = fcmService.getAps(title, body, (int) unreadCount);
        ApnsConfig apnsConfig = apnsConfigBuilder.setAps(aps)
                .build();

        MulticastMessage messages = MulticastMessage.builder()
                .addAllTokens(deviceList)
                .setApnsConfig(apnsConfig)
                .setNotification(notification)
                .build();

        return new MulticastMessageWithTokens(deviceList, messages);
    }

    /**
     * 알림 전송
     *
     * @param memberNotification 저장된 알림
     */
    @Override
    @Transactional(readOnly = true)
    public void send(MemberNotification memberNotification) {
        // 알림을 보낼 대상 회원들
        Long memberId = memberNotification.getId()
                .getMember()
                .getId();

        List<String> deviceByMemberId = memberDeviceService.getDeviceByMemberId(memberId);

        NotificationDetails details = memberNotification.getId()
                .getDetails();

        long unreadCount = notificationRepository.findUnreadCountByMemberId(memberId);

        Long id = details.getId();
        String title = details.getTitle();
        String body = details.getBody();
        NotificationType type = details.getType();
        String value = details.getValue();

        Notification notification = fcmService.getNotification(title, body);
        NotificationSend notificationSend = new NotificationSend(id, title, body, type, value);

        ApnsConfig apnsConfig = fcmService.getApnsConfig(notificationSend, (int) unreadCount);

        MulticastMessage multicastMessage = MulticastMessage.builder()
                .addAllTokens(deviceByMemberId)
                .setApnsConfig(apnsConfig)
                .setNotification(notification)
                .build();

        fcmService.sendMessages(multicastMessage, deviceByMemberId);
    }
}
