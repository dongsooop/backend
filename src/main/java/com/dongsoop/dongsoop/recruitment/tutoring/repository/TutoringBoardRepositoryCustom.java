package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitment;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
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

    List<OpenedRecruitment> findOpenedRecruitmentsByMemberId(Long memberId, int limit);
}
