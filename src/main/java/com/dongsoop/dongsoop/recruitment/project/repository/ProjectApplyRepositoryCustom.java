package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.recruitment.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import java.util.Optional;

public interface ProjectApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);

    void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status);

    Optional<ApplyDetails> findApplyDetailsByBoardIdAndApplierId(Long boardId, Long applierId);
}
