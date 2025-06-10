package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardDetails;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardOverview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface ProjectBoardRepositoryCustom {

    List<ProjectBoardOverview> findProjectBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                Pageable pageable);

    List<ProjectBoardOverview> findProjectBoardOverviewsByPage(Pageable pageable);

    Optional<ProjectBoardDetails> findProjectBoardDetails(Long projectBoardId);
}
