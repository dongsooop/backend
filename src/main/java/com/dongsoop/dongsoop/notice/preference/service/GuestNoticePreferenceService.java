package com.dongsoop.dongsoop.notice.preference.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.util.List;
import java.util.Set;

public interface GuestNoticePreferenceService {

    void updateDepartments(String fid, String deviceToken, Set<DepartmentType> departmentTypes);

    List<DepartmentType> getDepartmentTypes(String fid, String deviceToken);
}
