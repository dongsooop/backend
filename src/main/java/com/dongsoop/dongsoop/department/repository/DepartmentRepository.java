package com.dongsoop.dongsoop.department.repository;

import com.dongsoop.dongsoop.department.Department;
import com.dongsoop.dongsoop.department.DepartmentType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, DepartmentType> {
}
