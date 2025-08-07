package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import java.util.List;

public interface MemberDeviceRepositoryCustom {

    List<String> getMemberDeviceByDepartment(Department department);
}
