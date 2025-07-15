package com.dongsoop.dongsoop.recruitment.board.projection;

import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.apply.project.entity.QProjectApply;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.project.entity.QProjectBoard;
import com.dongsoop.dongsoop.recruitment.board.project.entity.QProjectBoardDepartment;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.stereotype.Component;

@Component
public class ProjectRecruitmentProjection implements RecruitmentProjection {

    private static final QProjectBoard board = QProjectBoard.projectBoard;
    private static final QProjectApply apply = QProjectApply.projectApply;
    private static final QProjectBoardDepartment boardDepartment = QProjectBoardDepartment.projectBoardDepartment;

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
                apply.id.member.countDistinct().intValue(),
                Expressions.constant(viewType),
                Expressions.constant(isAlreadyApplied));
    }
}
