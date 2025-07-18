package com.dongsoop.dongsoop.recruitment.board.study.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface StudyBoardRepositoryCustom {

    List<RecruitmentOverview> findStudyBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                             Pageable pageable);

    List<RecruitmentOverview> findStudyBoardOverviewsByPage(Pageable pageable);

    Optional<RecruitmentDetails> findBoardDetailsByIdAndViewType(Long studyBoardId,
                                                                 RecruitmentViewType viewType,
                                                                 boolean isAlreadyApplied);
}
