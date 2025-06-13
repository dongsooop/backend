package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.recruitment.study.entity.QStudyApply;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyApplyRepositoryCustomImpl implements StudyApplyRepositoryCustom {

    private static final QStudyApply studyApply = QStudyApply.studyApply;

    private final JPAQueryFactory queryFactory;

    public boolean existsByBoardIdAndMemberId(Long boardId, Long memberId) {
        return queryFactory.selectOne()
                .from(studyApply)
                .where(studyApply.id.studyBoard.id.eq(boardId)
                        .and(studyApply.id.member.id.eq(memberId)))
                .fetchFirst() != null;
    }
}
