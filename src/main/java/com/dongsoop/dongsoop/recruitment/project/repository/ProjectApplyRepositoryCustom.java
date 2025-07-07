package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;

public interface ProjectApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);

    void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status);
}
