package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringApply;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TutoringApplyRepositoryCustomImpl implements TutoringApplyRepositoryCustom {

    private static final QTutoringApply tutoringApply = QTutoringApply.tutoringApply;

    private final JPAQueryFactory queryFactory;

    public boolean existsByBoardIdAndMemberId(Long boardId, Long memberId) {
        return queryFactory.selectOne()
                .from(tutoringApply)
                .where(tutoringApply.id.tutoringBoard.id.eq(boardId)
                        .and(tutoringApply.id.member.id.eq(memberId)))
                .fetchFirst() != null;
    }

    public void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status) {
        queryFactory.update(tutoringApply)
                .where(tutoringApply.id.tutoringBoard.id.eq(boardId)
                        .and(tutoringApply.id.member.id.eq(memberId)))
                .set(tutoringApply.status, status)
                .execute();
    }
}
