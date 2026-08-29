package com.dongsoop.dongsoop.notice.preference.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.notice.preference.dto.GuestDepartmentResponse;
import java.util.Set;

public interface GuestNoticePreferenceService {

    void updateDepartments(String anonymousKey, Set<DepartmentType> departmentTypes);

    GuestDepartmentResponse getDepartments(String anonymousKey);
}
