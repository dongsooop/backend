package com.dongsoop.dongsoop.recruitment.board.project.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.project.dto.CreateProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ProjectBoardService {

    List<RecruitmentOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType, Pageable pageable);

    List<RecruitmentOverview> getBoardByPage(Pageable pageable);

    ProjectBoard create(CreateProjectBoardRequest request);

    RecruitmentDetails getBoardDetailsById(Long boardId);
}
