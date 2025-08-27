package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import java.util.List;
import java.util.Set;

public interface MemberDeviceRepositoryCustom {

    List<MemberDeviceDto> getAllMemberDevice();

    List<MemberDeviceDto> getMemberDeviceByDepartment(Department department);

    List<String> getMemberDeviceTokenByMemberIds(Set<Long> memberIds);

    List<String> getMemberDeviceTokenByMemberId(Long memberId);
}
