package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyApply;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyApplyRepositoryCustomImpl implements StudyApplyRepositoryCustom {

    private static final QStudyApply studyApply = QStudyApply.studyApply;

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsByBoardIdAndMemberId(Long boardId, Long memberId) {
        return queryFactory.selectOne()
                .from(studyApply)
                .where(studyApply.id.studyBoard.id.eq(boardId)
                        .and(studyApply.id.member.id.eq(memberId)))
                .fetchFirst() != null;
    }

    @Override
    public void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status) {
        queryFactory.update(studyApply)
                .where(studyApply.id.studyBoard.id.eq(boardId)
                        .and(studyApply.id.member.id.eq(memberId)))
                .set(studyApply.status, status)
                .execute();
    }
}
