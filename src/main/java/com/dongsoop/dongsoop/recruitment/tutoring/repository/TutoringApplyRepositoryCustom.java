package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.member.entity.Member;

public interface TutoringApplyRepositoryCustom {

    boolean existsByBoardIdAndMember(Long boardId, Member member);
}
