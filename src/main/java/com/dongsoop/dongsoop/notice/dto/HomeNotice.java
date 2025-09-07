package com.dongsoop.dongsoop.notice.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;

public record HomeNotice(

        String title,
        String link,
        NoticeType type
) {
    public HomeNotice(String title, String link, DepartmentType departmentType) {
        this(title, link, getNoticeType(departmentType));
    }

    private static NoticeType getNoticeType(DepartmentType departmentType) {
        if (departmentType.isAllDepartment()) {
            return NoticeType.OFFICIAL;
        }

        return NoticeType.DEPARTMENT;
    }
}
