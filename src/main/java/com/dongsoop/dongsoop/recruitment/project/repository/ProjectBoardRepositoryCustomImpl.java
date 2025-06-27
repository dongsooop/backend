package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitment;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectApply;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectBoard;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.projection.ProjectRecruitmentProjection;
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
public class ProjectBoardRepositoryCustomImpl implements ProjectBoardRepositoryCustom {

    private static final QProjectBoard projectBoard = QProjectBoard.projectBoard;

    private static final QProjectBoardDepartment projectBoardDepartment = QProjectBoardDepartment.projectBoardDepartment;

    private static final QProjectApply projectApply = QProjectApply.projectApply;

    private final JPAQueryFactory queryFactory;

    private final PageableUtil pageableUtil;

    private final ProjectRecruitmentProjection projection;

    @Override
    public List<RecruitmentOverview> findProjectBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                      Pageable pageable) {
        return queryFactory
                .select(projection.getRecruitmentOverviewExpression())
                .from(projectBoard)
                .leftJoin(projectApply)
                .on(hasMatchingProjectBoardId(projectApply.id.projectBoard.id))
                .leftJoin(projectBoardDepartment)
                .on(hasMatchingProjectBoardId(projectBoardDepartment.id.projectBoard.id))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(projectBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), projectBoard))
                .having(equalDepartmentType(departmentType))
                .fetch();
    }

    @Override
    public Optional<RecruitmentDetails> findBoardDetailsByIdAndViewType(Long projectBoardId,
                                                                        RecruitmentViewType viewType,
                                                                        boolean isAlreadyApplied) {
        RecruitmentDetails projectBoardDetails = queryFactory
                .select(projection.getRecruitmentDetailsExpression(viewType, isAlreadyApplied))
                .from(projectBoard)
                .leftJoin(projectApply)
                .on(hasMatchingProjectBoardId(projectApply.id.projectBoard.id))
                .leftJoin(projectBoardDepartment)
                .on(hasMatchingProjectBoardId(projectBoardDepartment.id.projectBoard.id))
                .groupBy(
                        projectBoard.id,
                        projectBoard.title,
                        projectBoard.content,
                        projectBoard.tags,
                        projectBoard.startAt,
                        projectBoard.endAt,
                        projectBoard.author.nickname,
                        projectBoard.createdAt,
                        projectBoard.updatedAt)
                .where(projectBoard.id.eq(projectBoardId))
                .fetchOne();

        return Optional.ofNullable(projectBoardDetails);
    }

    @Override
    public List<RecruitmentOverview> findProjectBoardOverviewsByPage(Pageable pageable) {
        return queryFactory
                .select(projection.getRecruitmentOverviewExpression())
                .from(projectBoard)
                .leftJoin(projectApply)
                .on(hasMatchingProjectBoardId(projectApply.id.projectBoard.id))
                .leftJoin(projectBoardDepartment)
                .on(hasMatchingProjectBoardId(projectBoardDepartment.id.projectBoard.id))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(projectBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), projectBoard))
                .fetch();
    }

    private BooleanExpression hasMatchingProjectBoardId(NumberPath<Long> projectBoardId) {
        return projectBoard.id.eq(projectBoardId);
    }

    private BooleanExpression equalDepartmentType(DepartmentType departmentType) {
        return Expressions.numberTemplate(
                        Integer.class,
                        "SUM(CASE WHEN {0} = {1} THEN 1 ELSE 0 END)",
                        projectBoardDepartment.id.department.id,
                        Expressions.constant(departmentType))
                .gt(0);
    }

    @Override
    public List<OpenedRecruitment> findOpenedRecruitmentsByMemberId(Long memberId, Pageable pageable) {
        return queryFactory
                .select(projection.getOpenedRecruitmentExpression())
                .from(projectBoard)
                .leftJoin(projectApply)
                .on(projectApply.id.projectBoard.id.eq(projectBoard.id)
                        .and(projectApply.id.member.id.eq(memberId)))
                .leftJoin(projectBoardDepartment)
                .on(hasMatchingProjectBoardId(projectBoardDepartment.id.projectBoard.id))
                .where(projectBoard.author.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(projectBoard.id)
                .orderBy(projectBoard.createdAt.desc())
                .fetch();
    }
}
