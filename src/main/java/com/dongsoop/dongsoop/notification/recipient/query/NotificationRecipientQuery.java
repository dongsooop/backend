package com.dongsoop.dongsoop.notification.recipient.query;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;

public interface NotificationRecipientQuery {
    List<MemberDeviceDto> findDevicesWithNotification(MemberDeviceFindCondition condition);
    List<Member> findMembersByRoleWithDevice(RoleType roleType);
}
