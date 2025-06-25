package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardOverview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface TutoringBoardRepositoryCustom {

    List<TutoringBoardOverview> findTutoringBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                  Pageable pageable);

    List<TutoringBoardOverview> findTutoringBoardOverviewsByPage(Pageable pageable);

    Optional<TutoringBoardDetails> findBoardDetailsByIdAndViewType(Long tutoringBoardId,
                                                                   RecruitmentViewType viewType,
                                                                   boolean isAlreadyApplied);

    List<ApplyRecruitment> findApplyRecruitmentsByMemberId(Long memberId, Pageable pageable);
}
