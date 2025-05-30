package com.dongsoop.dongsoop.project.repository;

import com.dongsoop.dongsoop.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.project.entity.ProjectBoardDepartment.ProjectBoardDepartmentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectBoardDepartmentRepository extends
        JpaRepository<ProjectBoardDepartment, ProjectBoardDepartmentId> {
}
