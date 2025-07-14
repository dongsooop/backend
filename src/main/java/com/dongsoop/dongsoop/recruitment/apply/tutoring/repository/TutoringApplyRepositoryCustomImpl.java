package com.dongsoop.dongsoop.recruitment.apply.tutoring.repository;

import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.apply.projection.TutoringRecruitmentApplyProjection;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.entity.QTutoringApply;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.QTutoringBoard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TutoringApplyRepositoryCustomImpl implements TutoringApplyRepositoryCustom {

    private static final QTutoringApply tutoringApply = QTutoringApply.tutoringApply;
    private static final QTutoringBoard tutoringBoard = QTutoringBoard.tutoringBoard;
    private static final QMember member = QMember.member;
    private static final QDepartment department = QDepartment.department;

    private final TutoringRecruitmentApplyProjection tutoringRecruitmentApplyProjection;

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsByBoardIdAndMemberId(Long boardId, Long memberId) {
        return queryFactory.selectOne()
                .from(tutoringApply)
                .where(tutoringApply.id.tutoringBoard.id.eq(boardId)
                        .and(tutoringApply.id.member.id.eq(memberId)))
                .fetchFirst() != null;
    }

    @Override
    public void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status) {
        queryFactory.update(tutoringApply)
                .where(tutoringApply.id.tutoringBoard.id.eq(boardId)
                        .and(tutoringApply.id.member.id.eq(memberId)))
                .set(tutoringApply.status, status)
                .execute();
    }

    @Override
    public Optional<ApplyDetails> findApplyDetailsByBoardIdAndApplierId(Long boardId, Long applierId) {
        ApplyDetails result = queryFactory.select(tutoringRecruitmentApplyProjection.getApplyDetailsExpression())
                .from(tutoringApply)
                .leftJoin(tutoringApply.id.tutoringBoard, tutoringBoard)
                .leftJoin(tutoringApply.id.member, member)
                .leftJoin(member.department, department)
                .where(tutoringApply.id.tutoringBoard.id.eq(boardId)
                        .and(tutoringApply.id.member.id.eq(applierId)))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
