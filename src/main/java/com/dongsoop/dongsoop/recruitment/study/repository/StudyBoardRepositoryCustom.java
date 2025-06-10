package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardDetails;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardOverview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface StudyBoardRepositoryCustom {

    List<StudyBoardOverview> findStudyBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                            Pageable pageable);

    List<StudyBoardOverview> findStudyBoardOverviewsByPage(Pageable pageable);

    Optional<StudyBoardDetails> findStudyBoardDetails(Long studyBoardId);
}
