package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import java.util.List;
import java.util.Set;

public interface MemberDeviceRepositoryCustom {

    List<String> getAllMemberDevice();

    List<String> getMemberDeviceByDepartment(Department department);

    List<String> getMemberDeviceTokenByMemberIds(Set<Long> memberIds);

    List<String> getMemberDeviceTokenByMemberId(Long memberId);
}
