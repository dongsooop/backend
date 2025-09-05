package com.dongsoop.dongsoop.home.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.home.dto.HomeDto;

public interface HomeService {

    HomeDto getHome(Long memberId, DepartmentType departmentType);
}
