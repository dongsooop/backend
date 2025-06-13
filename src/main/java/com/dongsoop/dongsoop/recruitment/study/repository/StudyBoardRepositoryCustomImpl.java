package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardDetails;
import com.dongsoop.dongsoop.recruitment.study.dto.StudyBoardOverview;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyApply;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyBoard;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyBoardDepartment;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyBoardRepositoryCustomImpl implements StudyBoardRepositoryCustom {

    private static final QStudyBoard studyBoard = QStudyBoard.studyBoard;

    private static final QStudyBoardDepartment studyBoardDepartment = QStudyBoardDepartment.studyBoardDepartment;

    private static final QStudyApply studyApply = QStudyApply.studyApply;

    private final JPAQueryFactory queryFactory;

    private final PageableUtil pageableUtil;

    public List<StudyBoardOverview> findStudyBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                   Pageable pageable) {
        return queryFactory
                .select(getBoardOverviewExpression())
                .from(studyBoard)
                .leftJoin(studyApply)
                .on(hasMatchingStudyBoardId(studyApply.id.studyBoard.id))
                .leftJoin(studyBoardDepartment)
                .on(hasMatchingStudyBoardId(studyBoardDepartment.id.studyBoard.id))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(studyBoard.id)
                .having(equalDepartmentType(departmentType))
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), studyBoard))
                .fetch();
    }

    public Optional<StudyBoardDetails> findBoardDetailsByIdAndViewType(Long studyBoardId,
                                                                       RecruitmentViewType viewType) {
        StudyBoardDetails studyBoardDetails = queryFactory
                .select(Projections.constructor(StudyBoardDetails.class,
                        studyBoard.id,
                        studyBoard.title,
                        studyBoard.content,
                        studyBoard.tags,
                        studyBoard.startAt,
                        studyBoard.endAt,
                        Expressions.stringTemplate("string_agg({0}, ',')", studyBoardDepartment.id.department.id),
                        studyBoard.author.nickname,
                        studyBoard.createdAt,
                        studyBoard.updatedAt,
                        studyApply.id.member.count().intValue(),
                        Expressions.constant(viewType)))
                .from(studyBoard)
                .leftJoin(studyApply)
                .on(hasMatchingStudyBoardId(studyApply.id.studyBoard.id))
                .leftJoin(studyBoardDepartment)
                .on(hasMatchingStudyBoardId(studyBoardDepartment.id.studyBoard.id))
                .groupBy(
                        studyBoard.id,
                        studyBoard.title,
                        studyBoard.content,
                        studyBoard.tags,
                        studyBoard.startAt,
                        studyBoard.endAt,
                        studyBoard.author.nickname,
                        studyBoard.createdAt,
                        studyBoard.updatedAt)
                .where(studyBoard.id.eq(studyBoardId))
                .fetchOne();

        return Optional.ofNullable(studyBoardDetails);
    }

    public List<StudyBoardOverview> findStudyBoardOverviewsByPage(Pageable pageable) {
        return queryFactory
                .select(getBoardOverviewExpression())
                .from(studyBoard)
                .leftJoin(studyApply)
                .on(hasMatchingStudyBoardId(studyApply.id.studyBoard.id))
                .leftJoin(studyBoardDepartment)
                .on(hasMatchingStudyBoardId(studyBoardDepartment.id.studyBoard.id))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(studyBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), studyBoard))
                .fetch();
    }

    private BooleanExpression hasMatchingStudyBoardId(NumberPath<Long> studyBoardId) {
        return studyBoard.id.eq(studyBoardId);
    }

    private Expression<StudyBoardOverview> getBoardOverviewExpression() {
        return Projections.constructor(StudyBoardOverview.class,
                studyBoard.id,
                studyApply.id.member.countDistinct().intValue(),
                studyBoard.startAt,
                studyBoard.endAt,
                studyBoard.title,
                studyBoard.content,
                studyBoard.tags,
                Expressions.stringTemplate("string_agg({0}, ',')",
                        studyBoardDepartment.id.department.id));
    }

    private BooleanExpression equalDepartmentType(DepartmentType departmentType) {
        return Expressions.numberTemplate(
                        Integer.class,
                        "SUM(CASE WHEN {0} = {1} THEN 1 ELSE 0 END)",
                        studyBoardDepartment.id.department.id,
                        Expressions.constant(departmentType))
                .gt(0);
    }
}
