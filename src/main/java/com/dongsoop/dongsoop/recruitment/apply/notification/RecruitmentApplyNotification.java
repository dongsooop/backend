package com.dongsoop.dongsoop.recruitment.apply.notification;

import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.FCMService;
import java.util.List;

public abstract class RecruitmentApplyNotification {

    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final FCMService fcmService;

    public RecruitmentApplyNotification(MemberDeviceRepositoryCustom memberDeviceRepositoryCustom,
                                        FCMService fcmService) {
        this.memberDeviceRepositoryCustom = memberDeviceRepositoryCustom;
        this.fcmService = fcmService;
    }

    protected abstract NotificationType getNotificationType();

    public void sendApplyNotification(Long boardId, String boardTitle, Long ownerId) {
        List<String> ownerDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberId(
                ownerId);

        String processBoardTitle = processBoardTitle(boardTitle);
        String body = "[" + processBoardTitle + "] 새로운 지원자가 있습니다. 확인해보세요!";

        fcmService.sendNotification(ownerDevice, "모집 지원자 알림", body, getNotificationType(), String.valueOf(boardId));
    }

    public void sendOutcomeNotification(Long boardId, String boardTitle, Long applierId) {
        List<String> applierDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberId(
                applierId);

        String processBoardTitle = processBoardTitle(boardTitle);
        String body = "[" + processBoardTitle + "] 모집 지원 결과가 등록되었습니다. 행운을 빌어요!";

        fcmService.sendNotification(applierDevice, "모집 결과 알림", body, getNotificationType(), String.valueOf(boardId));
    }

    private String processBoardTitle(String boardTitle) {
        if (boardTitle.length() <= 8) {
            return boardTitle;
        }

        return boardTitle.substring(0, 8) + "...";
    }
}
