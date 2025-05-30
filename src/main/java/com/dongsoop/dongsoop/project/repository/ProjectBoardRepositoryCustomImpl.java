package com.dongsoop.dongsoop.project.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.project.dto.ProjectBoardDetails;
import com.dongsoop.dongsoop.project.dto.ProjectBoardOverview;
import com.dongsoop.dongsoop.project.entity.QProjectBoard;
import com.dongsoop.dongsoop.project.entity.QProjectBoardApplication;
import com.dongsoop.dongsoop.project.entity.QProjectBoardDepartment;
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

    private static final QProjectBoardApplication projectBoardApplication = QProjectBoardApplication.projectBoardApplication;

    private final JPAQueryFactory queryFactory;

    private final PageableUtil pageableUtil;

    public List<ProjectBoardOverview> findProjectBoardOverviewsByPage(DepartmentType departmentType,
                                                                      Pageable pageable) {
        return queryFactory
                .select(Projections.constructor(ProjectBoardOverview.class,
                        projectBoard.id,
                        projectBoardApplication.id.member.countDistinct().intValue(),
                        projectBoard.startAt,
                        projectBoard.endAt,
                        projectBoard.title,
                        projectBoard.content,
                        projectBoard.tags))
                .from(projectBoard)
                .leftJoin(projectBoardApplication)
                .on(hasMatchingProjectBoardId(projectBoardApplication.id.projectBoard.id))
                .leftJoin(projectBoardDepartment)
                .on(hasMatchingProjectBoardId(projectBoardDepartment.id.projectBoard.id))
                .where(equalDepartmentType(departmentType))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(projectBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), projectBoard))
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
                        projectBoardApplication.id.member.count().intValue()))
                .from(projectBoard)
                .leftJoin(projectBoardApplication)
                .on(hasMatchingProjectBoardId(projectBoardApplication.id.projectBoard.id))
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

    private BooleanExpression hasMatchingProjectBoardId(NumberPath<Long> projectBoardId) {
        return projectBoard.id.eq(projectBoardId);
    }

    private BooleanExpression equalDepartmentType(DepartmentType departmentType) {
        return projectBoardDepartment.id.department.id.eq(departmentType);
    }
}
