package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectApply;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectApplyRepositoryCustomImpl implements ProjectApplyRepositoryCustom {

    private static final QProjectApply projectApply = QProjectApply.projectApply;

    private final JPAQueryFactory queryFactory;

    public boolean existsByBoardIdAndMember(Long boardId, Member member) {
        return queryFactory.selectOne()
                .from(projectApply)
                .where(projectApply.id.projectBoard.id.eq(boardId)
                        .and(projectApply.id.member.eq(member)))
                .fetchFirst() != null;
    }
}
