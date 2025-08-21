package com.dongsoop.dongsoop.recruitment.apply.study.repository;

import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.study.entity.StudyApply;
import java.util.Optional;

public interface StudyApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);

    Optional<ApplyDetails> findApplyDetailsByBoardIdAndApplierId(Long boardId, Long applierId);

    Optional<StudyApply> findByBoardIdAndApplierId(Long boardId, Long applierId);
}
