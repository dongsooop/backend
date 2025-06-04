package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoardDepartment.ProjectBoardDepartmentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectBoardDepartmentRepository extends
        JpaRepository<ProjectBoardDepartment, ProjectBoardDepartmentId> {
}
