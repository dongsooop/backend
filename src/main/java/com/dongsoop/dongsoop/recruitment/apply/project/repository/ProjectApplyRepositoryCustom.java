package com.dongsoop.dongsoop.recruitment.apply.project.repository;

import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.project.entity.ProjectApply;
import java.util.Optional;

public interface ProjectApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);

    Optional<ApplyDetails> findApplyDetailsByBoardIdAndApplierId(Long boardId, Long applierId);

    Optional<ProjectApply> findByBoardIdAndApplierId(Long boardId, Long applierId);
}
