package com.dongsoop.dongsoop.notice.dto;

import com.dongsoop.dongsoop.department.entity.Department;

public interface NoticeMaxIdByType {

    Department getDepartment();

    Long getMaxId();
}
