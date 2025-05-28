package com.dongsoop.dongsoop.project.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.project.dto.CreateProjectBoardRequest;
import com.dongsoop.dongsoop.project.dto.ProjectBoardDetails;
import com.dongsoop.dongsoop.project.dto.ProjectBoardOverview;
import com.dongsoop.dongsoop.project.entity.ProjectBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ProjectBoardService {

    ProjectBoard create(CreateProjectBoardRequest request);

    List<ProjectBoardOverview> getProjectBoardByPage(DepartmentType departmentType, Pageable pageable);

    ProjectBoardDetails getProjectBoardDetails(Long projectBoardId);
}
