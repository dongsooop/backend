package com.dongsoop.dongsoop.recruitment.study.repository;

public interface StudyApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
}
