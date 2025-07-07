package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;

public interface TutoringApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long member);

    void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status);
}
