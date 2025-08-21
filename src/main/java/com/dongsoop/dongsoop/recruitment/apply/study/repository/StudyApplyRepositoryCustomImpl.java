package com.dongsoop.dongsoop.recruitment.apply.study.repository;

import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.apply.projection.StudyRecruitmentApplyProjection;
import com.dongsoop.dongsoop.recruitment.apply.study.entity.QStudyApply;
import com.dongsoop.dongsoop.recruitment.board.study.entity.QStudyBoard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyApplyRepositoryCustomImpl implements StudyApplyRepositoryCustom {

    private static final QStudyApply studyApply = QStudyApply.studyApply;
    private static final QStudyBoard studyBoard = QStudyBoard.studyBoard;
    private static final QMember member = QMember.member;
    private static final QDepartment department = QDepartment.department;

    private final StudyRecruitmentApplyProjection studyRecruitmentApplyProjection;

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
        ApplyDetails result = queryFactory.select(studyRecruitmentApplyProjection.getApplyDetailsExpression())
                .from(studyApply)
                .leftJoin(studyApply.id.studyBoard, studyBoard)
                .leftJoin(studyApply.id.member, member)
                .leftJoin(member.department, department)
                .where(studyApply.id.studyBoard.id.eq(boardId)
                        .and(studyApply.id.member.id.eq(applierId)))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<String> findTitleByMemberIdAndBoardId(Long memberId, Long boardId) {
        String result = queryFactory.select(studyBoard.title)
                .from(studyApply)
                .innerJoin(studyApply.id.studyBoard, studyBoard)
                .where(studyApply.id.studyBoard.id.eq(boardId)
                        .and(studyApply.id.member.id.eq(memberId)))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
