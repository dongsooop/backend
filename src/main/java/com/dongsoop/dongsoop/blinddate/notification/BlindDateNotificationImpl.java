package com.dongsoop.dongsoop.blinddate.notification;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlindDateNotificationImpl implements BlindDateNotification {

    private static final String BLINDDATE_TITLE = "과팅 이벤트가 개최되었습니다\uD83C\uDF89";
    private static final String BLINDDATE_BODY = "지금 바로 참여하여 나와 잘 맞는 사람을 찾아보세요!\n* 이벤트 기간 내에만 참여 가능합니다.";

    private final NotificationSaveService notificationSaveService;
    private final NotificationSendService notificationSendService;
    private final MemberRepository memberRepository;

    @Override
    public void send() {
        List<Member> memberList = memberRepository.findByRoleTypeWithDevice(RoleType.ADMIN);
        if (memberList.isEmpty()) {
            return;
        }
        
        List<MemberNotification> memberNotificationList = notificationSaveService.saveAll(memberList,
                BLINDDATE_TITLE,
                BLINDDATE_BODY, NotificationType.BLINDDATE, "");

        notificationSendService.sendAll(memberNotificationList, NotificationType.BLINDDATE);
    }
}
