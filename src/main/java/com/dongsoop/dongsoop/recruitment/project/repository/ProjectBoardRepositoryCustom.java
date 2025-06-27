package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface ProjectBoardRepositoryCustom {

    List<RecruitmentOverview> findProjectBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                               Pageable pageable);

    List<RecruitmentOverview> findProjectBoardOverviewsByPage(Pageable pageable);

    Optional<RecruitmentDetails> findBoardDetailsByIdAndViewType(Long projectBoardId, RecruitmentViewType viewType,
                                                                 boolean isAlreadyApplied);
}
