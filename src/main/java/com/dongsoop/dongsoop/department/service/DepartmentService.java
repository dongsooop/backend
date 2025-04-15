package com.dongsoop.dongsoop.department.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;

public interface DepartmentService {

    Department getReferenceById(DepartmentType departmentType);
}
