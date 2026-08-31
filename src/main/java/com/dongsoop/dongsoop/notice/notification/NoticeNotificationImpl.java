package com.dongsoop.dongsoop.notice.notification;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.dto.DeviceSubscription;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.keyword.service.NoticeKeywordFilter;
import com.dongsoop.dongsoop.notice.keyword.service.NoticeKeywordService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.util.HashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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

        // 대상 기기와 키워드를 앞에서 한 번씩만 읽고, 공지별 분배는 메모리에서 한다.
        // 학과마다 조회하면 같은 학과에서 공지가 여러 건 나올 때 같은 결과를 반복해서 읽는다
        Set<DepartmentType> departmentTypes = noticeDetailSet.stream()
                .map(notice -> notice.getDepartment().getId())
                .collect(Collectors.toSet());

        List<MemberDevice> allDevices = memberDeviceRepository.searchDevicesByDepartments(departmentTypes);
        Map<DepartmentType, List<MemberDevice>> devicesByDepartment = groupByDepartment(allDevices, departmentTypes);
        NoticeKeywordFilter keywordFilter = noticeKeywordService.loadFilter(allDevices);

        // 공지마다 대상 기기가 다르므로 저장-발송은 공지 단위로 묶는다.
        // 여러 공지의 기기를 합쳐서 회원 단위로 발송하면 A 공지 알림이 B 공지만 구독한 기기로 가고,
        // 같은 기기가 여러 공지를 통과했을 때 토큰이 중복되어 푸시가 여러 번 간다
        for (Notice notice : noticeDetailSet) {
            // 기기별 키워드로 걸러 회원/비회원 발송이 같은 기준을 쓰게 한다
            List<MemberDevice> subscribedDevices = devicesByDepartment.get(notice.getDepartment().getId());
            List<MemberDevice> allowedDevices = keywordFilter.apply(subscribedDevices,
                    notice.getNoticeDetails().getTitle());

            // 공지 알림 저장 후, 키워드를 통과한 기기에만 전송한다.
            // 회원의 전체 기기를 다시 조회하는 sendAll 을 쓰면 걸러낸 기기까지 발송된다
            List<MemberNotification> memberNotificationList = save(notice, allowedDevices);
            notificationSendService.sendAllToDevices(memberNotificationList, allowedDevices);

            // 비회원 발송: 알림함을 남기지 않고 푸시만 보낸다
            sendToGuests(notice, allowedDevices);
        }
    }

    /**
     * 조회한 기기를 학과별로 나눈다.
     *
     * <p>기기 조회와 구독 관계 조회를 각각 한 번으로 끝내고 분배는 여기서 처리한다.
     * 대학 공지는 구독 여부와 무관하게 전체 기기가 대상이라 그대로 담는다.
     *
     * @param devices         대상 학과를 구독한 기기 전체
     * @param departmentTypes 이번에 발송할 공지들의 학과
     */
    private Map<DepartmentType, List<MemberDevice>> groupByDepartment(List<MemberDevice> devices,
                                                                      Set<DepartmentType> departmentTypes) {
        Map<Long, Set<DepartmentType>> subscribedDepartments = loadSubscribedDepartments(devices);

        Map<DepartmentType, List<MemberDevice>> devicesByDepartment = new HashMap<>();
        for (DepartmentType departmentType : departmentTypes) {
            if (departmentType.isAllDepartment()) {
                devicesByDepartment.put(departmentType, devices);
                continue;
            }

            devicesByDepartment.put(departmentType, devices.stream()
                    .filter(device -> subscribedDepartments.getOrDefault(device.getId(), Set.of())
                            .contains(departmentType))
                    .toList());
        }

        return devicesByDepartment;
    }

    private Map<Long, Set<DepartmentType>> loadSubscribedDepartments(List<MemberDevice> devices) {
        List<Long> deviceIds = devices.stream()
                .map(MemberDevice::getId)
                .toList();

        return memberDeviceRepository.findSubscriptionsByDeviceIds(deviceIds).stream()
                .collect(Collectors.groupingBy(DeviceSubscription::deviceId,
                        Collectors.mapping(DeviceSubscription::departmentType, Collectors.toSet())));
    }

    /**
     * 공지사항을 알림으로 변환
     *
     * <p>device_notice_preference로 해당 학과를 구독한 기기 중 회원 소유 기기만 대상으로 한다.
     * 한 회원이 여러 기기로 구독했더라도 알림함/배지는 회원 단위로 한 번만 생성된다.
     * 따라서 한 기기라도 키워드를 통과했으면 그 회원의 알림함에는 남는다.
     *
     * @param notice            공지사항
     * @param subscribedDevices 키워드 조건을 통과한 구독 기기 (회원+비회원)
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

        String noticeLink = universityDomain + notice.getNoticeDetails().getLink();
        return notificationSaveService.saveAll(targetList, title, body, NotificationType.NOTICE, noticeLink);
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
     * @param subscribedDevices 키워드 조건을 통과한 구독 기기 (회원+비회원)
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
