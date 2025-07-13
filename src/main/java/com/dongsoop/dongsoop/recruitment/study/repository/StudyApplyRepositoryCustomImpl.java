package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.recruitment.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.projection.StudyRecruitmentProjection;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyApply;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyApplyRepositoryCustomImpl implements StudyApplyRepositoryCustom {

    private static final QStudyApply studyApply = QStudyApply.studyApply;
    private static final QMember member = QMember.member;
    private static final QDepartment department = QDepartment.department;

    private final StudyRecruitmentProjection studyRecruitmentProjection;

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

    @Override
    public Optional<ApplyDetails> findApplyDetailsByBoardIdAndApplierId(Long boardId, Long applierId) {
        ApplyDetails result = queryFactory.select(studyRecruitmentProjection.getApplyDetailsExpression())
                .from(studyApply)
                .leftJoin(studyApply.id.member, member)
                .leftJoin(member.department, department)
                .where(studyApply.id.studyBoard.id.eq(boardId)
                        .and(studyApply.id.member.id.eq(applierId)))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
