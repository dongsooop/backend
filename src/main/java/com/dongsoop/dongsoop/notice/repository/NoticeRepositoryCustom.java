package com.dongsoop.dongsoop.notice.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.notice.dto.HomeNotice;
import com.dongsoop.dongsoop.notice.entity.Notice;
import java.util.Collection;
import java.util.List;

public interface NoticeRepositoryCustom {

    List<Notice> findRecentNoticesIncludingDeleted(Department department, int limit);

    List<HomeNotice> searchHomeNotices(DepartmentType departmentType);

    List<HomeNotice> searchHomeNotices();

    List<HomeNotice> searchHomeNotices(Collection<DepartmentType> departmentTypes);
}
