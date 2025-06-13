package com.dongsoop.dongsoop.recruitment.project.repository;

public interface ProjectApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
}
