package com.dongsoop.dongsoop.recruitment.study.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.study.dto.CreateStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardDetails;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardOverview;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface StudyBoardService {

    List<StudyBoardOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType, Pageable pageable);

    List<StudyBoardOverview> getBoardByPage(Pageable pageable);

    StudyBoard create(CreateStudyBoardRequest request);

    StudyBoardDetails getBoardDetailsById(Long boardId);
}
