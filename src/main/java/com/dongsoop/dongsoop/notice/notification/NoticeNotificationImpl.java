package com.dongsoop.dongsoop.notice.notification;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.keyword.service.NoticeKeywordService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeNotificationImpl implements NoticeNotification {

    // Firebase Admin SDK가 MulticastMessage 하나에 허용하는 상한. 넘기면 build()에서 예외가 발생한다
    private static final int FCM_MULTICAST_MAX_TOKENS = 500;

    private static final Long NON_SAVE_NOTIFICATION_ID = -1L;

    private final NotificationSaveService notificationSaveService;
    private final NotificationSendService notificationSendService;
    private final MemberDeviceRepository memberDeviceRepository;
    private final NoticeKeywordService noticeKeywordService;

    @Value("${university.domain}")
    private String universityDomain;

    /**
     * 공지사항 알림 전송
     *
     * @param noticeDetailSet 공지-학과 매핑 Set
     */
    @Async
    public void send(Set<Notice> noticeDetailSet) {
        if (noticeDetailSet == null || noticeDetailSet.isEmpty()) {
            return;
        }

        // 공지별로 device_notice_preference 구독 기기를 한 번만 조회해 회원/비회원 발송에 공유한다
        Map<Notice, List<MemberDevice>> devicesByNotice = new LinkedHashMap<>();
        for (Notice notice : noticeDetailSet) {
            devicesByNotice.put(notice, memberDeviceRepository.searchDevicesByDepartment(notice.getDepartment().getId()));
        }

        // 공지 알림 저장 후 알림 리스트 반환
        List<MemberNotification> memberNotificationList = saveMemberNotification(devicesByNotice);

        // 공지별 메시지 변환 후 전송
        notificationSendService.sendAll(memberNotificationList, NotificationType.NOTICE);

        // 비회원 발송: 알림함을 남기지 않고 푸시만 보낸다
        devicesByNotice.forEach(this::sendToGuests);
    }

    /**
     * 공지사항 알림을 DB에 저장
     *
     * @param devicesByNotice 공지별 구독 기기(회원+비회원) 목록
     */
    private List<MemberNotification> saveMemberNotification(Map<Notice, List<MemberDevice>> devicesByNotice) {
        return devicesByNotice.entrySet().stream()
                .map(entry -> save(entry.getKey(), entry.getValue()))
                .flatMap(List::stream)
                .toList();
    }

    /**
     * 공지사항을 알림으로 변환
     *
     * <p>device_notice_preference로 해당 학과를 구독한 기기 중 회원 소유 기기만 대상으로 한다.
     * 한 회원이 여러 기기로 구독했더라도 알림함/배지는 회원 단위로 한 번만 생성된다.
     *
     * @param notice            공지사항
     * @param subscribedDevices 해당 학과를 구독한 전체 기기 (회원+비회원)
     * @return 공지 알림 리스트
     */
    private List<MemberNotification> save(Notice notice, List<MemberDevice> subscribedDevices) {
        Department department = notice.getDepartment();
        String departmentName = department.getId().getName();

        String title = generateTitle(departmentName);
        String body = notice.getNoticeDetails().getTitle();
        // 구독 기기 중 회원 소유(member != null)만 남기고, 같은 회원이 기기를 여러 개
        // 구독했어도 알림함 저장은 회원 단위로 한 번만 되도록 distinct 처리한다
        List<Member> targetList = subscribedDevices.stream()
                .map(MemberDevice::getMember)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Member> filteredList = noticeKeywordService.filterMembersByKeyword(targetList, body);
        String noticeLink = universityDomain + notice.getNoticeDetails().getLink();
        return notificationSaveService.saveAll(filteredList, title, body, NotificationType.NOTICE, noticeLink);
    }

    private String generateTitle(String departmentName) {
        return String.format("[%s] 공지 알림", departmentName);
    }

    /**
     * 비회원 디바이스에 공지 푸시를 직접 전송한다.
     *
     * <p>비회원은 알림함({@code MemberNotification})을 갖지 않으므로 저장 없이 발송만 한다.
     * 회원 발송이 이미 끝난 뒤 호출되므로, 여기서 발생한 예외가 회원 발송에 영향을 주지 않도록 삼킨다.
     *
     * @param notice            공지사항
     * @param subscribedDevices 해당 학과를 구독한 전체 기기 (회원+비회원)
     */
    private void sendToGuests(Notice notice, List<MemberDevice> subscribedDevices) {
        try {
            // 구독 기기 중 회원 소유가 아닌(member == null) 게스트 기기만 대상으로 한다.
            // 회원 소유 기기는 save()가 이미 알림함 저장 + 배지 계산까지 처리했다
            List<String> tokens = subscribedDevices.stream()
                    .filter(device -> device.getMember() == null)
                    .map(MemberDevice::getDeviceToken)
                    .toList();

            if (tokens.isEmpty()) {
                return;
            }

            Department department = notice.getDepartment();
            String title = notice.getNoticeDetails().getTitle();

            NotificationSend notificationSend = new NotificationSend(
                    NON_SAVE_NOTIFICATION_ID,
                    generateTitle(department.getId().getName()),
                    title,
                    NotificationType.NOTICE,
                    universityDomain + notice.getNoticeDetails().getLink());

            // FCM_MULTICAST_MAX_TOKENS(500)개씩 잘라서 보낸다. 한 번에 넘기면
            // Firebase Admin SDK가 MulticastMessage.build()에서 예외를 던진다
            for (int fromIndex = 0; fromIndex < tokens.size(); fromIndex += FCM_MULTICAST_MAX_TOKENS) {
                int toIndex = Math.min(fromIndex + FCM_MULTICAST_MAX_TOKENS, tokens.size());
                notificationSendService.send(tokens.subList(fromIndex, toIndex), notificationSend);
            }
        } catch (Exception exception) {
            log.error("Failed to send notice push to guest devices", exception);
        }
    }
}
