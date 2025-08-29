package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.exception.NotificationNotFoundException;
import com.dongsoop.dongsoop.notification.repository.NotificationDetailsRepository;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDetailsRepository notificationDetailsRepository;
    private final MemberService memberService;
    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final FCMService fcmService;

    @Override
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
    public Map<NotificationDetails, List<Member>> listToMap(List<MemberNotification> memberNotificationList) {
        return memberNotificationList.stream().collect(Collectors.groupingBy(
                notification -> notification.getId().getDetails(),
                Collectors.mapping(
                        notification -> notification.getId().getMember(),
                        Collectors.toList()
                )));
    }

    @Override
    public void send(Map<NotificationDetails, List<Member>> memberByNotification) {
        memberByNotification.entrySet().stream()
                .map(this::toMulticastMessage)
                .forEach(fcmService::sendMessages);
    }

    /**
     * MulticastMessage 변환
     *
     * @param notificationEntry { 공지 세부: 회원 리스트 } 구조인 Entry
     * @return MulticastMessage 변환한 메시지
     */
    private MulticastMessage toMulticastMessage(Entry<NotificationDetails, List<Member>> notificationEntry) {
        NotificationDetails details = notificationEntry.getKey();
        List<Member> memberList = notificationEntry.getValue();
        List<String> deviceTokens = memberDeviceRepositoryCustom.getDeviceByMembers(memberList);

        // 공지별 회원 알림 전송
        Long notificationId = details.getId();
        String title = details.getTitle();
        String body = details.getBody();
        String noticeId = details.getValue();

        NotificationSend notificationSend = new NotificationSend(notificationId, title, body,
                details.getType(), noticeId);
        ApnsConfig apnsConfig = fcmService.getApnsConfig(notificationSend);

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        return MulticastMessage.builder()
                .addAllTokens(deviceTokens)
                .setApnsConfig(apnsConfig)
                .setNotification(notification)
                .build();
    }

    @Override
    public List<NotificationOverview> getNotifications(Pageable pageable) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        return notificationRepository.getMemberNotifications(requesterId, pageable);
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
