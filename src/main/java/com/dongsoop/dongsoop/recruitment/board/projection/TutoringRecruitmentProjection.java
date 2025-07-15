package com.dongsoop.dongsoop.recruitment.board.projection;

import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.entity.QTutoringApply;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.QTutoringBoard;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.stereotype.Component;

@Component
public class TutoringRecruitmentProjection implements RecruitmentProjection {

    private static final QTutoringBoard board = QTutoringBoard.tutoringBoard;
    private static final QTutoringApply apply = QTutoringApply.tutoringApply;

    @Override
    public ConstructorExpression<RecruitmentOverview> getRecruitmentOverviewExpression() {
        return Projections.constructor(RecruitmentOverview.class,
                board.id,
                apply.id.member.countDistinct().intValue(),
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
                apply.id.member.countDistinct().intValue(),
                Expressions.constant(viewType),
                Expressions.constant(isAlreadyApplied));
    }
}
