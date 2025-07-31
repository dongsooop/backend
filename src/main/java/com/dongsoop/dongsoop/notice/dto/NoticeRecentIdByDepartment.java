package com.dongsoop.dongsoop.notice.dto;

import com.dongsoop.dongsoop.department.entity.Department;

public interface NoticeRecentIdByDepartment {

    Department getDepartment();

    Long getRecentId();
}
