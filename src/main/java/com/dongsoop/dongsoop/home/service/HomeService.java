package com.dongsoop.dongsoop.home.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.home.dto.HomeDto;
import java.util.Set;

public interface HomeService {

    HomeDto getHome(Long memberId, DepartmentType departmentType, String fid, String deviceToken);

    HomeDto getHome(Set<DepartmentType> departmentTypes, String fid, String deviceToken);
}
