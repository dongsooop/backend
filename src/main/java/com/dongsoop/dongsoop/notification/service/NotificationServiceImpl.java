package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepositoryCustom;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final MemberDeviceRepositoryCustom memberDeviceRepositoryCustom;
    private final FCMService fcmService;

    @Override
    public void sendNotificationByDepartment(Department department, Set<NoticeDetails> noticeDetailsSet) {
        List<String> deviceTokenList = getDeviceByDepartment(department);

        noticeDetailsSet.forEach(noticeDetails ->
                fcmService.sendNotification(deviceTokenList, noticeDetails.getTitle(), noticeDetails.getWriter()));
    }

    private List<String> getDeviceByDepartment(Department department) {
        if (department.getId().equals(DepartmentType.DEPT_1001)) {
            return memberDeviceRepositoryCustom.getAllMemberDevice();
        }

        return memberDeviceRepositoryCustom.getMemberDeviceByDepartment(department);
    }

    @Override
    public void sendNotificationForChat(Set<Long> chatroomMemberIdSet, String senderName, String message) {
        // 사용자 id를 통해 FCM 토큰을 가져옴
        List<String> participantsDevice = memberDeviceRepositoryCustom.getMemberDeviceTokenByMemberId(
                chatroomMemberIdSet);

        fcmService.sendNotification(participantsDevice, senderName, message);
    }
}
