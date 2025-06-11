package com.dongsoop.dongsoop.recruitment.project.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.project.dto.CreateProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardDetails;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardOverview;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ProjectBoardService {

    List<ProjectBoardOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType, Pageable pageable);

    List<ProjectBoardOverview> getBoardByPage(Pageable pageable);

    ProjectBoard create(CreateProjectBoardRequest request);

    ProjectBoardDetails getBoardDetailsById(Long boardId);
}
