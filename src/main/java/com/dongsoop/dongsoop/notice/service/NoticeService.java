package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.DepartmentType;
import com.dongsoop.dongsoop.notice.dto.NoticeListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeService {

    Page<NoticeListResponse> getNoticeByDepartmentType(DepartmentType departmentType, Pageable pageable);
}
