package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceResponse;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import java.time.LocalDateTime;
import java.util.List;

public interface MemberDeviceRepositoryCustom {

    List<MemberDeviceDto> findDevicesWithNotification(MemberDeviceFindCondition condition);

    List<String> getDeviceByMemberId(Long memberId);

    List<MemberDeviceResponse> findDeviceListByMemberId(Long memberId, String currentDeviceToken);

    long deleteExpiredDevices(LocalDateTime cutoff);

    List<MemberDevice> searchGuestDevicesByDepartment(DepartmentType departmentType);
}
