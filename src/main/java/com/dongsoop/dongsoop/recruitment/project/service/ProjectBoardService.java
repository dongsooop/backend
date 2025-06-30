package com.dongsoop.dongsoop.recruitment.project.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.project.dto.CreateProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ProjectBoardService {

    List<RecruitmentOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType, Pageable pageable);

    List<RecruitmentOverview> getBoardByPage(Pageable pageable);

    ProjectBoard create(CreateProjectBoardRequest request);

    RecruitmentDetails getBoardDetailsById(Long boardId);
}
