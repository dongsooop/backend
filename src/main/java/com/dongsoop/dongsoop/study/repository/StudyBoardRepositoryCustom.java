package com.dongsoop.dongsoop.study.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.study.dto.StudyBoardDetails;
import com.dongsoop.dongsoop.study.dto.StudyBoardOverview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface StudyBoardRepositoryCustom {

    List<StudyBoardOverview> findStudyBoardOverviewsByPage(DepartmentType departmentType, Pageable pageable);

    Optional<StudyBoardDetails> findStudyBoardDetails(Long studyBoardId);
}
