package com.dongsoop.dongsoop.notice.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.notice.dto.HomeNotice;
import java.util.List;

public interface NoticeRepositoryCustom {

    List<HomeNotice> searchHomeNotices(DepartmentType departmentType);

    List<HomeNotice> searchHomeNotices();
}
