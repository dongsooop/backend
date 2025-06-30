package com.dongsoop.dongsoop.recruitment.study.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.study.dto.CreateStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface StudyBoardService {

    List<RecruitmentOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType, Pageable pageable);

    List<RecruitmentOverview> getBoardByPage(Pageable pageable);

    StudyBoard create(CreateStudyBoardRequest request);

    RecruitmentDetails getBoardDetailsById(Long boardId);
}
