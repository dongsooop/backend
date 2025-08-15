package com.dongsoop.dongsoop.department.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.util.List;

public interface DepartmentService {

    Department getReferenceById(DepartmentType departmentType);

    List<Department> getAllDepartments();
}
