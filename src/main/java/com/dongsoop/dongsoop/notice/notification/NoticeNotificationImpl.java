package com.dongsoop.dongsoop.notice.notification;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.keyword.service.NoticeKeywordService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.util.Collection;
import java.util.List;
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
    private final MemberRepository memberRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final NoticeKeywordService noticeKeywordService;

    @Value("${university.domain}")
    private String universityDomain;

    /**
     * 공지사항 알림 전송
     *
     * @param noticeDetailSet 공지-회원 매핑 Set
     */
    @Async
    public void send(Set<Notice> noticeDetailSet) {
        if (noticeDetailSet == null || noticeDetailSet.isEmpty()) {
            return;
        }

        // 공지 알림 저장 후 알림 리스트 반환
        List<MemberNotification> memberNotificationList = saveMemberNotification(noticeDetailSet);

        // 공지별 메시지 변환 후 전송
        notificationSendService.sendAll(memberNotificationList, NotificationType.NOTICE);

        // 비회원 발송: 알림함을 남기지 않고 푸시만 보낸다
        noticeDetailSet.forEach(this::sendToGuests);
    }

    /**
     * 공지사항 알림을 DB에 저장
     *
     * @param noticeSet { 학과: 공지사항 세부 } 구조인 Notice Set
     */
    private List<MemberNotification> saveMemberNotification(Set<Notice> noticeSet) {
        return noticeSet.stream().map(this::save)
                .flatMap(Collection::stream)
                .toList();
    }

    /**
     * 공지사항을 알림으로 변환
     *
     * @param notice 공지사항
     * @return 공지 알림 리스트
     */
    private List<MemberNotification> save(Notice notice) {
        Department department = notice.getDepartment();
        String departmentName = department.getId().getName();

        String title = generateTitle(departmentName);
        String body = notice.getNoticeDetails().getTitle();
        List<Member> targetList = getMemberByDepartment(department);

        List<Member> filteredList = noticeKeywordService.filterMembersByKeyword(targetList, body);
        String noticeLink = universityDomain + notice.getNoticeDetails().getLink();
        return notificationSaveService.saveAll(filteredList, title, body, NotificationType.NOTICE, noticeLink);
    }

    private String generateTitle(String departmentName) {
        return String.format("[%s] 공지 알림", departmentName);
    }

    private List<Member> getMemberByDepartment(Department department) {
        if (department.getId().isAllDepartment()) {
            return memberRepository.searchAllByDeviceNotEmpty();
        }

        return memberRepository.searchAllByDepartmentAndDeviceNotEmpty(department);
    }

    /**
     * 비회원 디바이스에 공지 푸시를 직접 전송한다.
     *
     * <p>비회원은 알림함({@code MemberNotification})을 갖지 않으므로 저장 없이 발송만 한다.
     * 회원 발송이 이미 끝난 뒤 호출되므로, 여기서 발생한 예외가 회원 발송에 영향을 주지 않도록 삼킨다.
     */
    private void sendToGuests(Notice notice) {
        try {
            Department department = notice.getDepartment();
            List<MemberDevice> devices = memberDeviceRepository.searchGuestDevicesByDepartment(department.getId());

            String title = notice.getNoticeDetails().getTitle();
            List<MemberDevice> filtered = noticeKeywordService.filterDevicesByKeyword(devices, title);
            if (filtered.isEmpty()) {
                return;
            }

            List<String> tokens = filtered.stream()
                    .map(MemberDevice::getDeviceToken)
                    .toList();

            NotificationSend notificationSend = new NotificationSend(
                    NON_SAVE_NOTIFICATION_ID,
                    generateTitle(department.getId().getName()),
                    title,
                    NotificationType.NOTICE,
                    universityDomain + notice.getNoticeDetails().getLink());

            for (int fromIndex = 0; fromIndex < tokens.size(); fromIndex += FCM_MULTICAST_MAX_TOKENS) {
                int toIndex = Math.min(fromIndex + FCM_MULTICAST_MAX_TOKENS, tokens.size());
                notificationSendService.send(tokens.subList(fromIndex, toIndex), notificationSend);
            }
        } catch (Exception exception) {
            log.error("Failed to send notice push to guest devices", exception);
        }
    }
}
