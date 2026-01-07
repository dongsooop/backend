package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import java.util.List;

public interface MemberDeviceRepositoryCustom {

    List<MemberDeviceDto> findDevicesWithNotification(MemberDeviceFindCondition condition);

    List<String> getDeviceByMemberId(Long memberId);
}
