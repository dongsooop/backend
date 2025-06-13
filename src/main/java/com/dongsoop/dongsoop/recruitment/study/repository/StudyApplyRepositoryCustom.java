package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.member.entity.Member;

public interface StudyApplyRepositoryCustom {

    boolean existsByBoardIdAndMember(Long boardId, Member member);
}
