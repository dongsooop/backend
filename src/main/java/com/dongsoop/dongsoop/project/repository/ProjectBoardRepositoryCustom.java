package com.dongsoop.dongsoop.project.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.project.dto.ProjectBoardDetails;
import com.dongsoop.dongsoop.project.dto.ProjectBoardOverview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface ProjectBoardRepositoryCustom {

    List<ProjectBoardOverview> findProjectBoardOverviewsByPage(DepartmentType departmentType, Pageable pageable);

    Optional<ProjectBoardDetails> findProjectBoardDetails(Long projectBoardId);
}
