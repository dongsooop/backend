package com.dongsoop.dongsoop.recruitment.projection;

import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringApply;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringBoard;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.stereotype.Component;

@Component
public class TutoringRecruitmentProjection implements RecruitmentProjection {

    private static final QTutoringBoard board = QTutoringBoard.tutoringBoard;
    private static final QTutoringApply apply = QTutoringApply.tutoringApply;
    private static final QMember member = QMember.member;
    private static final QDepartment department = QDepartment.department;

    @Override
    public ConstructorExpression<RecruitmentOverview> getRecruitmentOverviewExpression() {
        return Projections.constructor(RecruitmentOverview.class,
                board.id,
                apply.id.member.count().intValue(),
                board.startAt,
                board.endAt,
                board.title,
                board.content,
                board.tags,
                board.department.id.stringValue());
    }

    @Override
    public ConstructorExpression<RecruitmentDetails> getRecruitmentDetailsExpression(
            RecruitmentViewType viewType, boolean isAlreadyApplied) {
        return Projections.constructor(RecruitmentDetails.class,
                board.id,
                board.title,
                board.content,
                board.tags,
                board.startAt,
                board.endAt,
                board.department.id.stringValue(),
                board.author.nickname,
                board.createdAt.as("createdAt"),
                board.updatedAt.as("updatedAt"),
                apply.id.member.count().intValue(),
                Expressions.constant(viewType),
                Expressions.constant(isAlreadyApplied));
    }

    @Override
    public ConstructorExpression<ApplyDetails> getApplyDetailsExpression() {
        return Projections.constructor(ApplyDetails.class,
                board.id,
                member.id,
                member.nickname,
                department.name,
                apply.applyTime,
                apply.introduction,
                apply.motivation);
    }
}
