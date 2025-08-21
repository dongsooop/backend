package com.dongsoop.dongsoop.recruitment.apply.tutoring.repository;

import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.entity.TutoringApply;
import java.util.Optional;

public interface TutoringApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long member);

    Optional<ApplyDetails> findApplyDetailsByBoardIdAndApplierId(Long boardId, Long applierId);

    Optional<TutoringApply> findByBoardIdAndApplierId(Long boardId, Long applierId);
}
