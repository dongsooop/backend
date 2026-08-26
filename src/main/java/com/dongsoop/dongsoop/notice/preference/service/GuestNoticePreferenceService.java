package com.dongsoop.dongsoop.notice.preference.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.notice.preference.dto.GuestDepartmentResponse;

public interface GuestNoticePreferenceService {

    void updateDepartment(String anonymousKey, DepartmentType departmentType);

    GuestDepartmentResponse getDepartment(String anonymousKey);
}
