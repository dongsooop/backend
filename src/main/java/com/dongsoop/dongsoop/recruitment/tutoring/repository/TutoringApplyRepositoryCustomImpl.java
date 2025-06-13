package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringApply;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TutoringApplyRepositoryCustomImpl implements TutoringApplyRepositoryCustom {

    private static final QTutoringApply tutoringApply = QTutoringApply.tutoringApply;

    private final JPAQueryFactory queryFactory;

    public boolean existsByBoardIdAndMember(Long boardId, Member member) {
        return queryFactory.selectOne()
                .from(tutoringApply)
                .where(tutoringApply.id.tutoringBoard.id.eq(boardId)
                        .and(tutoringApply.id.member.eq(member)))
                .fetchFirst() != null;
    }
}
