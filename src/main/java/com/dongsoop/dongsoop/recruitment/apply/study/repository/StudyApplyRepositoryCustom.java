package com.dongsoop.dongsoop.recruitment.apply.study.repository;

import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import java.util.Optional;

public interface StudyApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);

    void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status);

    Optional<ApplyDetails> findApplyDetailsByBoardIdAndApplierId(Long boardId, Long applierId);

    Optional<String> findTitleByMemberIdAndBoardId(Long memberId, Long boardId);
}
