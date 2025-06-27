package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitment;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.projection.StudyRecruitmentProjection;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyApply;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyBoard;
import com.dongsoop.dongsoop.recruitment.study.entity.QStudyBoardDepartment;
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

    private final StudyRecruitmentProjection projection;

    @Override
    public List<RecruitmentOverview> findStudyBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                    Pageable pageable) {
        return queryFactory
                .select(projection.getRecruitmentOverviewExpression())
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

    @Override
    public Optional<RecruitmentDetails> findBoardDetailsByIdAndViewType(Long studyBoardId,
                                                                        RecruitmentViewType viewType,
                                                                        boolean isAlreadyApplied) {
        RecruitmentDetails details = queryFactory
                .select(projection.getRecruitmentDetailsExpression(viewType, isAlreadyApplied))
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

        return Optional.ofNullable(details);
    }

    @Override
    public List<RecruitmentOverview> findStudyBoardOverviewsByPage(Pageable pageable) {
        return queryFactory
                .select(projection.getRecruitmentOverviewExpression())
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

    private BooleanExpression equalDepartmentType(DepartmentType departmentType) {
        return Expressions.numberTemplate(
                        Integer.class,
                        "SUM(CASE WHEN {0} = {1} THEN 1 ELSE 0 END)",
                        studyBoardDepartment.id.department.id,
                        Expressions.constant(departmentType))
                .gt(0);
    }

    @Override
    public List<OpenedRecruitment> findOpenedRecruitmentsByMemberId(Long memberId, Pageable pageable) {
        return queryFactory
                .select(projection.getOpenedRecruitmentExpression())
                .from(studyBoard)
                .leftJoin(studyApply)
                .on(studyApply.id.studyBoard.id.eq(studyBoard.id)
                        .and(studyApply.id.member.id.eq(memberId)))
                .leftJoin(studyBoardDepartment)
                .on(hasMatchingStudyBoardId(studyBoardDepartment.id.studyBoard.id))
                .where(studyBoard.author.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(studyBoard.id)
                .orderBy(studyBoard.createdAt.desc())
                .fetch();
    }
}
