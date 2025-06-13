package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.recruitment.project.entity.QProjectApply;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectApplyRepositoryCustomImpl implements ProjectApplyRepositoryCustom {

    private static final QProjectApply projectApply = QProjectApply.projectApply;

    private final JPAQueryFactory queryFactory;

    public boolean existsByBoardIdAndMemberId(Long boardId, Long memberId) {
        return queryFactory.selectOne()
                .from(projectApply)
                .where(projectApply.id.projectBoard.id.eq(boardId)
                        .and(projectApply.id.member.id.eq(memberId)))
                .fetchFirst() != null;
    }
}
