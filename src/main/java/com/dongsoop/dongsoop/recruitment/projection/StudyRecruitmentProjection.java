package com.dongsoop.dongsoop.recruitment.projection;

import com.dongsoop.dongsoop.mypage.dto.MyRecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyApply;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyBoard;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyBoardDepartment;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class StudyRecruitmentProjection implements RecruitmentProjection {

    private static final QStudyBoard board = QStudyBoard.studyBoard;
    private static final QStudyApply apply = QStudyApply.studyApply;
    private static final QStudyBoardDepartment boardDepartment = QStudyBoardDepartment.studyBoardDepartment;

    @Override
    public ConstructorExpression<MyRecruitmentOverview> getApplyRecruitmentExpression() {
        return Projections.constructor(MyRecruitmentOverview.class,
                board.id,
                JPAExpressions.select(apply.id.member.countDistinct().intValue())
                        .from(apply)
                        .where(board.id.eq(apply.id.studyBoard.id)),
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
                Expressions.constant(RecruitmentType.STUDY),
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
}
