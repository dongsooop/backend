package com.dongsoop.dongsoop.recruitment.board.projection;

import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.apply.study.entity.QStudyApply;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.study.entity.QStudyBoard;
import com.dongsoop.dongsoop.recruitment.board.study.entity.QStudyBoardDepartment;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.stereotype.Component;

@Component
public class StudyRecruitmentProjection implements RecruitmentProjection {

    private static final QStudyBoard board = QStudyBoard.studyBoard;
    private static final QStudyApply apply = QStudyApply.studyApply;
    private static final QStudyBoardDepartment boardDepartment = QStudyBoardDepartment.studyBoardDepartment;

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
                board.author.id,
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
