package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.recruitment.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import java.util.Optional;

public interface TutoringApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long member);

    void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status);

    Optional<ApplyDetails> findApplyDetailsByBoardIdAndApplierId(Long boardId, Long applierId);
}
