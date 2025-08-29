package com.dongsoop.dongsoop.recruitment.apply.notification;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import java.util.List;
import java.util.Map;

public abstract class RecruitmentApplyNotification {

    private static final Integer TITLE_TRUNCATION_LENGTH = 8;

    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final NotificationService notificationService;

    public RecruitmentApplyNotification(MemberDeviceRepositoryCustom memberDeviceRepositoryCustom,
                                        NotificationService notificationService) {
        this.memberDeviceRepositoryCustom = memberDeviceRepositoryCustom;
        this.notificationService = notificationService;
    }

    protected abstract NotificationType getApplyNotificationType();

    protected abstract NotificationType getOutcomeNotificationType();

    public void sendApplyNotification(Long boardId, String boardTitle, Long ownerId) {
        List<MemberDeviceDto> ownerDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberId(
                ownerId);

        String processBoardTitle = processBoardTitle(boardTitle);
        String title = "모집 지원자 알림";
        String body = "[" + processBoardTitle + "] 새로운 지원자가 있습니다. 확인해보세요!";
        NotificationType type = getApplyNotificationType();

        String value = String.valueOf(boardId);

        notificationService.save(ownerDevice, title, body, type, value);
    }

    public void sendOutcomeNotification(Long boardId, String boardTitle, Long applierId) {
        List<MemberDeviceDto> applierDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberId(
                applierId);

        String processBoardTitle = processBoardTitle(boardTitle);
        String title = "모집 결과 알림";
        String body = "[" + processBoardTitle + "] 모집 지원 결과가 등록되었습니다. 행운을 빌어요!";
        NotificationType type = getOutcomeNotificationType();

        // 저장
        List<MemberNotification> memberNotificationList = notificationService.save(applierDevice, title, body, type,
                String.valueOf(boardId));

        // 저장된 알림 -> Map 변환
        Map<NotificationDetails, List<Member>> memberByNotification = notificationService.listToMap(
                memberNotificationList);

        // 알림 전송
        notificationService.send(memberByNotification);
    }

    private String processBoardTitle(String boardTitle) {
        if (boardTitle.length() <= TITLE_TRUNCATION_LENGTH) {
            return boardTitle;
        }

        return boardTitle.substring(0, TITLE_TRUNCATION_LENGTH) + "...";
    }
}
