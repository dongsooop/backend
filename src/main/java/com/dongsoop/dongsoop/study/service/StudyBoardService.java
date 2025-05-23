package com.dongsoop.dongsoop.study.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.study.dto.CreateStudyBoardRequest;
import com.dongsoop.dongsoop.study.dto.StudyBoardDetails;
import com.dongsoop.dongsoop.study.dto.StudyBoardOverview;
import com.dongsoop.dongsoop.study.entity.StudyBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface StudyBoardService {

    StudyBoard create(CreateStudyBoardRequest request);

    List<StudyBoardOverview> getStudyBoardByPage(DepartmentType departmentType, Pageable pageable);

    StudyBoardDetails getStudyBoardDetails(Long studyBoardId);
}
