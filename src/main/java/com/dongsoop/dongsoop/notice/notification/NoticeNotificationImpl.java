package com.dongsoop.dongsoop.notice.notification;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoticeNotificationImpl implements NoticeNotification {

    private final FCMService fcmService;
    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final NotificationService notificationService;

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

        // 공지별 공지 그룹핑
        Map<NotificationDetails, List<Member>> notificationByDepartment = groupByDetails(memberNotificationList);

        // 공지별 메시지 변환 후 전송
        notificationService.send(notificationByDepartment);
    }

    /**
     * 공지사항 알림을 DB에 저장
     *
     * @param noticeSet { 학과: 공지사항 세부 } 구조인 Notice Set
     */
    private List<MemberNotification> saveMemberNotification(Set<Notice> noticeSet) {
        return noticeSet.stream().map((notice) -> {
                    Department department = notice.getDepartment();
                    String departmentName = department.getId().getName();

                    String title = generateTitle(departmentName);
                    String body = notice.getNoticeDetails().getTitle();
                    List<MemberDeviceDto> deviceTokens = getMemberDeviceDtoByDepartment(department);

                    String noticeLink = universityDomain + notice.getNoticeDetails().getLink();
                    return notificationService.save(deviceTokens, title, body, NotificationType.NOTICE, noticeLink);
                })
                .flatMap(Collection::stream)
                .toList();
    }

    /**
     * 공지별 회원 그룹핑
     *
     * @param memberNotificationList 공지-회원 매핑 리스트
     * @return Map<NotificationDetails, List<Member>> 공지별 회원 Map
     */
    private Map<NotificationDetails, List<Member>> groupByDetails(List<MemberNotification> memberNotificationList) {
        return memberNotificationList.stream().collect(Collectors.groupingBy(
                memberNotification -> memberNotification.getId().getDetails(),
                Collectors.mapping(
                        memberNotification -> memberNotification.getId().getMember(),
                        Collectors.toList()
                )
        ));
    }

    private String generateTitle(String departmentName) {
        return String.format("[%s] 공지 알림", departmentName);
    }

    private List<MemberDeviceDto> getMemberDeviceDtoByDepartment(Department department) {
        if (department.getId().isAllDepartment()) {
            return memberDeviceRepositoryCustom.getAllMemberDevice();
        }

        return memberDeviceRepositoryCustom.getMemberDeviceByDepartment(department);
    }
}
