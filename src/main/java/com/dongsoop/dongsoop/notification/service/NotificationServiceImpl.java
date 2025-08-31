package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.MulticastMessageWithTokens;
import com.dongsoop.dongsoop.notification.dto.NotificationList;
import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.dto.NotificationUnread;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.exception.NotificationNotFoundException;
import com.dongsoop.dongsoop.notification.repository.NotificationDetailsRepository;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDetailsRepository notificationDetailsRepository;
    private final MemberService memberService;
    private final FCMService fcmService;
    private final MemberDeviceService memberDeviceService;

    @Override
    @Transactional
    public List<MemberNotification> save(List<MemberDeviceDto> memberDeviceDtoList, String title, String body,
                                         NotificationType type,
                                         String value) {
        NotificationDetails details = NotificationDetails.builder()
                .title(title)
                .body(body)
                .type(type)
                .value(value)
                .build();

        notificationDetailsRepository.save(details);

        List<MemberNotification> memberNotificationList = memberDeviceDtoList.stream()
                .map((memberDevice) -> new MemberNotification(details, memberDevice.member()))
                .toList();

        return notificationRepository.saveAll(memberNotificationList);
    }

    @Override
    public Map<NotificationDetails, List<Long>> listToMap(List<MemberNotification> memberNotificationList) {
        return memberNotificationList.stream().collect(Collectors.groupingBy(
                notification -> notification.getId().getDetails(),
                Collectors.mapping(
                        notification -> notification.getId().getMember().getId(),
                        Collectors.toList()
                )));
    }

    @Override
    public void send(List<String> deviceTokenList, NotificationSend notificationSend) {
        fcmService.sendNotification(deviceTokenList, notificationSend);
    }

    /**
     * 알림 전송
     *
     * @param memberNotificationList 저장된 알림 리스트
     */
    @Override
    @Transactional(readOnly = true)
    public void send(List<MemberNotification> memberNotificationList) {
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
                .flatMap((map) ->
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
        String noticeId = details.getValue();

        // 캐싱된 회원 id별 디바이스 토큰
        Map<Long, List<String>> deviceTokens = memberIdDevices.entrySet().stream()
                .filter(entry -> memberIdList.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 공지 알림 전송 시 정보
        NotificationSend notificationSend = new NotificationSend(notificationId, title, body,
                details.getType(), noticeId);

        // 알림 객체 생성
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        ApnsConfig.Builder apnsConfigBuilder = ApnsConfig.builder()
                .putCustomData("type", notificationSend.type().toString())
                .putCustomData("value", notificationSend.value())
                .putCustomData("id", String.valueOf(notificationSend.id()));

        // 회원별 MulticastMessage 생성
        return memberIdList.stream()
                .map(memberId -> {
                    Long unreadCount = unreadCountByMember.getOrDefault(memberId, 0L);

                    return generateMulticastMessage(memberId, title, body, deviceTokens, notification, unreadCount,
                            apnsConfigBuilder);
                });
    }

    /**
     * MulticastMessage 생성
     *
     * @param memberId          회원 ID
     * @param title             알림 제목
     * @param body              알림 내용
     * @param deviceByMember    회원별 디바이스
     * @param notification      알림 객체
     * @param apnsConfigBuilder APNS 설정 빌더
     * @return MulticastMessage 생성한 메시지
     */
    private MulticastMessageWithTokens generateMulticastMessage(Long memberId, String title, String body,
                                                                Map<Long, List<String>> deviceByMember,
                                                                Notification notification,
                                                                long unreadCount,
                                                                ApnsConfig.Builder apnsConfigBuilder) {
        // APNS 생성
        Aps aps = fcmService.getAps(title, body, (int) unreadCount);
        ApnsConfig apnsConfig = apnsConfigBuilder.setAps(aps)
                .build();

        // 회원 디바이스 목록 가져오기
        List<String> deviceList = deviceByMember.get(memberId);
        MulticastMessage messages = MulticastMessage.builder()
                .addAllTokens(deviceList)
                .setApnsConfig(apnsConfig)
                .setNotification(notification)
                .build();

        return new MulticastMessageWithTokens(deviceList, messages);
    }

    @Override
    public NotificationOverview getNotifications(Pageable pageable) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        Long unreadCount = notificationRepository.findUnreadCountByMemberId(requesterId);
        List<NotificationList> notificationLists = notificationRepository.getMemberNotifications(requesterId,
                pageable);

        return new NotificationOverview(notificationLists, unreadCount);
    }

    @Override
    @Transactional
    public void deleteMemberNotification(Long id) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        MemberNotification memberNotification = notificationRepository.findByMemberIdAndNotificationId(requesterId, id)
                .orElseThrow(NotificationNotFoundException::new);

        memberNotification.delete();
    }

    @Override
    @Transactional
    public void read(Long id) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        MemberNotification memberNotification = notificationRepository.findByMemberIdAndNotificationId(requesterId, id)
                .orElseThrow(NotificationNotFoundException::new);

        memberNotification.read();
    }
}
