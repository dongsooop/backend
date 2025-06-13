package com.dongsoop.dongsoop.recruitment.tutoring.repository;

public interface TutoringApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long member);
}
