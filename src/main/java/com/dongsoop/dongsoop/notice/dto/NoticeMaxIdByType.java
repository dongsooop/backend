package com.dongsoop.dongsoop.notice.dto;

import com.dongsoop.dongsoop.department.DepartmentType;

public interface NoticeMaxIdByType {

    DepartmentType getType();

    Long getMaxId();
}
