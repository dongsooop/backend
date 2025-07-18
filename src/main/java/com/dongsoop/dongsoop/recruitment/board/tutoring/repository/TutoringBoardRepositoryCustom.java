package com.dongsoop.dongsoop.recruitment.board.tutoring.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface TutoringBoardRepositoryCustom {

    List<RecruitmentOverview> findTutoringBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                Pageable pageable);

    List<RecruitmentOverview> findTutoringBoardOverviewsByPage(Pageable pageable);

    Optional<RecruitmentDetails> findBoardDetailsByIdAndViewType(Long tutoringBoardId,
                                                                 RecruitmentViewType viewType,
                                                                 boolean isAlreadyApplied);
}
