package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardDetails;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardOverview;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectApply;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectBoard;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectBoardDepartment;
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
public class ProjectBoardRepositoryCustomImpl implements ProjectBoardRepositoryCustom {

    private static final QProjectBoard projectBoard = QProjectBoard.projectBoard;

    private static final QProjectBoardDepartment projectBoardDepartment = QProjectBoardDepartment.projectBoardDepartment;

    private static final QProjectApply projectApply = QProjectApply.projectApply;

    private final JPAQueryFactory queryFactory;

    private final PageableUtil pageableUtil;

    public List<ProjectBoardOverview> findProjectBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                       Pageable pageable) {
        return queryFactory
                .select(getBoardOverviewExpression())
                .from(projectBoard)
                .leftJoin(projectApply)
                .on(hasMatchingProjectBoardId(projectApply.id.projectBoard.id))
                .leftJoin(projectBoardDepartment)
                .on(hasMatchingProjectBoardId(projectBoardDepartment.id.projectBoard.id))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(projectBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), projectBoard))
                .having(
                        Expressions.numberTemplate(
                                        Integer.class,
                                        "SUM(CASE WHEN {0} = {1} THEN 1 ELSE 0 END)",
                                        projectBoardDepartment.id.department.id,
                                        Expressions.constant(departmentType))
                                .gt(0))
                .fetch();
    }

    public Optional<ProjectBoardDetails> findProjectBoardDetails(Long projectBoardId) {
        ProjectBoardDetails projectBoardDetails = queryFactory
                .select(Projections.constructor(ProjectBoardDetails.class,
                        projectBoard.id,
                        projectBoard.title,
                        projectBoard.content,
                        projectBoard.tags,
                        projectBoard.startAt,
                        projectBoard.endAt,
                        Expressions.stringTemplate("string_agg({0}, ',')", projectBoardDepartment.id.department.id),
                        projectBoard.author.nickname,
                        projectBoard.createdAt,
                        projectBoard.updatedAt,
                        projectApply.id.member.count().intValue()))
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

    public List<ProjectBoardOverview> findProjectBoardOverviewsByPage(Pageable pageable) {
        return queryFactory
                .select(getBoardOverviewExpression())
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

    private Expression<ProjectBoardOverview> getBoardOverviewExpression() {
        return Projections.constructor(ProjectBoardOverview.class,
                projectBoard.id,
                projectApply.id.member.countDistinct().intValue(),
                projectBoard.startAt,
                projectBoard.endAt,
                projectBoard.title,
                projectBoard.content,
                projectBoard.tags,
                Expressions.stringTemplate("string_agg({0}, ',')",
                        projectBoardDepartment.id.department.id));
    }
}
