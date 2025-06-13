package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.member.entity.Member;

public interface ProjectApplyRepositoryCustom {

    boolean existsByBoardIdAndMember(Long boardId, Member member);
}
