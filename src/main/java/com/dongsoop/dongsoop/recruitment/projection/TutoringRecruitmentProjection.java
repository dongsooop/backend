package com.dongsoop.dongsoop.recruitment.projection;

import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitment;
import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringApply;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringBoard;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class TutoringRecruitmentProjection implements RecruitmentProjection {

    private static final QTutoringBoard board = QTutoringBoard.tutoringBoard;
    private static final QTutoringApply apply = QTutoringApply.tutoringApply;

    @Override
    public ConstructorExpression<ApplyRecruitment> getApplyRecruitmentExpression() {
        return Projections.constructor(ApplyRecruitment.class,
                board.id,
                JPAExpressions.select(apply.id.member.countDistinct().intValue())
                        .from(apply)
                        .where(board.id.eq(apply.id.tutoringBoard.id)),
                board.startAt,
                board.endAt,
                board.title,
                board.content,
                board.tags,
                board.department.id.stringValue(),
                Expressions.constant(RecruitmentType.TUTORING),
                board.createdAt,
                isRecruiting());
    }

    @Override
    public ConstructorExpression<OpenedRecruitment> getOpenedRecruitmentExpression() {
        return Projections.constructor(OpenedRecruitment.class,
                board.id,
                apply.id.member.countDistinct().intValue(),
                board.startAt,
                board.endAt,
                board.title,
                board.content,
                board.tags,
                board.department.id.stringValue(),
                Expressions.constant(RecruitmentType.TUTORING),
                board.createdAt,
                isRecruiting());
    }

    private BooleanExpression isRecruiting() {
        return board.endAt.gt(LocalDateTime.now())
                .and(board.startAt.lt(LocalDateTime.now()));
    }

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
}
