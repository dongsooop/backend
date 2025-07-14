package com.dongsoop.dongsoop.recruitment.apply.tutoring.repository;

import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import java.util.Optional;

public interface TutoringApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long member);

    void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status);

    Optional<ApplyDetails> findApplyDetailsByBoardIdAndApplierId(Long boardId, Long applierId);
}
