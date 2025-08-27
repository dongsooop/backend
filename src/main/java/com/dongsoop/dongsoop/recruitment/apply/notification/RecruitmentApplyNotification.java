package com.dongsoop.dongsoop.recruitment.apply.notification;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import java.util.List;

public abstract class RecruitmentApplyNotification {

    private static final Integer TITLE_TRUNCATION_LENGTH = 8;

    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final FCMService fcmService;
    private final NotificationService notificationService;

    public RecruitmentApplyNotification(MemberDeviceRepositoryCustom memberDeviceRepositoryCustom,
                                        FCMService fcmService,
                                        NotificationService notificationService) {
        this.memberDeviceRepositoryCustom = memberDeviceRepositoryCustom;
        this.fcmService = fcmService;
        this.notificationService = notificationService;
    }

    protected abstract NotificationType getNotificationType();

    public void sendApplyNotification(Long boardId, String boardTitle, Long ownerId) {
        List<MemberDeviceDto> ownerDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberId(
                ownerId);

        String processBoardTitle = processBoardTitle(boardTitle);
        String title = "모집 지원자 알림";
        String body = "[" + processBoardTitle + "] 새로운 지원자가 있습니다. 확인해보세요!";

        saveNotification(ownerDevice, title, body, boardId);
    }

    public void sendOutcomeNotification(Long boardId, String boardTitle, Long applierId) {
        List<MemberDeviceDto> applierDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberId(
                applierId);

        String processBoardTitle = processBoardTitle(boardTitle);
        String title = "모집 결과 알림";
        String body = "[" + processBoardTitle + "] 모집 지원 결과가 등록되었습니다. 행운을 빌어요!";

        saveNotification(applierDevice, title, body, boardId);
    }

    private void saveNotification(List<MemberDeviceDto> memberDeviceDtos, String title, String body,
                                  Long boardId) {
        NotificationType notificationType = getNotificationType();
        String value = String.valueOf(boardId);

        List<String> deviceTokenList = memberDeviceDtos.stream()
                .map(MemberDeviceDto::deviceToken)
                .toList();

        fcmService.sendNotification(deviceTokenList, title, body, notificationType, value);
        notificationService.save(memberDeviceDtos, title, body, notificationType, value);
    }

    private String processBoardTitle(String boardTitle) {
        if (boardTitle.length() <= TITLE_TRUNCATION_LENGTH) {
            return boardTitle;
        }

        return boardTitle.substring(0, TITLE_TRUNCATION_LENGTH) + "...";
    }
}
