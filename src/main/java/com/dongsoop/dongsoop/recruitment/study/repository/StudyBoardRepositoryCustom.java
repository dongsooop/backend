package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitment;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardDetails;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardOverview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface StudyBoardRepositoryCustom {

    List<StudyBoardOverview> findStudyBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                            Pageable pageable);

    List<StudyBoardOverview> findStudyBoardOverviewsByPage(Pageable pageable);

    Optional<StudyBoardDetails> findBoardDetailsByIdAndViewType(Long studyBoardId,
                                                                RecruitmentViewType viewType,
                                                                boolean isAlreadyApplied);

    List<ApplyRecruitment> findApplyRecruitmentsByMemberId(Long memberId, Pageable pageable);

    List<OpenedRecruitment> findOpenedRecruitmentsByMemberId(Long memberId, Pageable pageable);
}
