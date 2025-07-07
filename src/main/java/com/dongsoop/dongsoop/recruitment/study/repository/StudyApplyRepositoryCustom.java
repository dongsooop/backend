package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;

public interface StudyApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);

    void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status);
}
