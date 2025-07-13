package com.dongsoop.dongsoop.recruitment.projection;

import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyApply;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyBoard;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyBoardDepartment;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.stereotype.Component;

@Component
public class StudyRecruitmentProjection implements RecruitmentProjection {

    private static final QStudyBoard board = QStudyBoard.studyBoard;
    private static final QStudyApply apply = QStudyApply.studyApply;
    private static final QStudyBoardDepartment boardDepartment = QStudyBoardDepartment.studyBoardDepartment;
    private static final QMember member = QMember.member;
    private static final QDepartment department = QDepartment.department;

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
