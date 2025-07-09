package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectApply;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectApplyRepositoryCustomImpl implements ProjectApplyRepositoryCustom {

    private static final QProjectApply projectApply = QProjectApply.projectApply;

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsByBoardIdAndMemberId(Long boardId, Long memberId) {
        return queryFactory.selectOne()
                .from(projectApply)
                .where(projectApply.id.projectBoard.id.eq(boardId)
                        .and(projectApply.id.member.id.eq(memberId)))
                .fetchFirst() != null;
    }

    @Override
    public void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status) {
        queryFactory.update(projectApply)
                .where(projectApply.id.projectBoard.id.eq(boardId)
                        .and(projectApply.id.member.id.eq(memberId)))
                .set(projectApply.status, status)
                .execute();
    }
}
