package com.dongsoop.dongsoop.recruitment.projection;

import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.mypage.dto.MyRecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectApply;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectBoard;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectBoardDepartment;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class ProjectRecruitmentProjection implements RecruitmentProjection {

    private static final QProjectBoard board = QProjectBoard.projectBoard;
    private static final QProjectApply apply = QProjectApply.projectApply;
    private static final QProjectBoardDepartment boardDepartment = QProjectBoardDepartment.projectBoardDepartment;
    private static final QDepartment department = QDepartment.department;
    private static final QMember member = QMember.member;

    @Override
    public ConstructorExpression<MyRecruitmentOverview> getApplyRecruitmentExpression() {
        return Projections.constructor(MyRecruitmentOverview.class,
                board.id,
                JPAExpressions.select(apply.id.member.countDistinct().intValue())
                        .from(apply)
                        .where(board.id.eq(apply.id.projectBoard.id)),
                board.startAt,
                board.endAt,
                board.title,
                board.content,
                board.tags,
                Expressions.stringTemplate("string_agg({0}, ',')",
                        boardDepartment.id.department.id),
                Expressions.constant(RecruitmentType.PROJECT),
                board.createdAt,
                isRecruiting());
    }

    @Override
    public ConstructorExpression<MyRecruitmentOverview> getOpenedRecruitmentExpression() {
        return Projections.constructor(MyRecruitmentOverview.class,
                board.id,
                apply.id.member.countDistinct().intValue(),
                board.startAt,
                board.endAt,
                board.title,
                board.content,
                board.tags,
                Expressions.stringTemplate("string_agg({0}, ',')",
                        boardDepartment.id.department.id),
                Expressions.constant(RecruitmentType.PROJECT),
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
                apply.id.member.countDistinct().intValue(),
                board.startAt,
                board.endAt,
                board.title,
                board.content,
                board.tags,
                Expressions.stringTemplate("string_agg({0}, ',')",
                        boardDepartment.id.department.id));
    }

    @Override
    public ConstructorExpression<RecruitmentDetails> getRecruitmentDetailsExpression(RecruitmentViewType viewType,
                                                                                     boolean isAlreadyApplied) {
        return Projections.constructor(RecruitmentDetails.class,
                board.id,
                board.title,
                board.content,
                board.tags,
                board.startAt,
                board.endAt,
                Expressions.stringTemplate("string_agg({0}, ',')", boardDepartment.id.department.id),
                board.author.nickname,
                board.createdAt,
                board.updatedAt,
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
