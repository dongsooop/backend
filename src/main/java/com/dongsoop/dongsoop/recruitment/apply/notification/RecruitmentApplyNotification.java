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

        String body = "[" + boardTitle + "] 글에 새로운 지원자가 있습니다. 확인해보세요!";

        fcmService.sendNotification(ownerDevice, "모집 지원자 알림", body, getNotificationType(), String.valueOf(boardId));
    }
}
