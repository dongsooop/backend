package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.notice.dto.NoticeListResponse;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeService {

    Page<NoticeListResponse> getNoticeByDepartmentType(DepartmentType departmentType, Pageable pageable);

    Map<Department, Long> getNoticeRecentIdMap();
}
