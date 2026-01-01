package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import java.util.Collection;
import java.util.List;

public interface MemberDeviceRepositoryCustom {

    List<MemberDeviceDto> getAllMemberDevice();

    List<MemberDeviceDto> getMemberDeviceByDepartment(Department department);

    List<MemberDeviceDto> getMemberDeviceTokenByMemberIds(Collection<Long> memberIds);

    List<MemberDeviceDto> findDevicesWithNotification(MemberDeviceFindCondition condition);

    List<MemberDeviceDto> getMemberDeviceTokenByMemberId(Long memberId);

    List<String> getDeviceByMemberId(Long memberId);
}
