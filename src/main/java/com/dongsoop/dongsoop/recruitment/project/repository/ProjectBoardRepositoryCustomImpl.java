package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardDetails;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardOverview;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectApply;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectBoard;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectBoardDepartment;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
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

    /**
     * 학과별로 모집중인 상태의 프로젝트 모집 게시판 목록을 페이지 단위로 조회합니다.
     *
     * @param departmentType 학과 타입
     * @param pageable       페이지 정보
     * @return 모집중인 프로젝트 모집 게시판 목록
     */
    @Override
    public List<ProjectBoardOverview> findProjectBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                       Pageable pageable) {
        return queryFactory
                .select(getBoardOverviewExpression())
                .from(projectBoard)
                .leftJoin(projectApply)
                .on(hasMatchingProjectBoardId(projectApply.id.projectBoard.id))
                .leftJoin(projectBoardDepartment)
                .on(hasMatchingProjectBoardId(projectBoardDepartment.id.projectBoard.id))
                .where(isRecruiting())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(projectBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), projectBoard))
                .having(equalDepartmentType(departmentType))
                .fetch();
    }

    /**
     * 특정 프로젝트 모집 게시판 ID와 뷰 타입에 따라 게시판 상세 정보를 조회합니다.
     *
     * @param projectBoardId   프로젝트 모집 게시판 ID
     * @param viewType         조회자 타입 (예: OWNER, MEMBER, GUEST)
     * @param isAlreadyApplied 현재 멤버가 이미 신청했는지 여부
     * @return 프로젝트 모집 게시판 상세 정보
     */
    @Override
    public Optional<ProjectBoardDetails> findBoardDetailsByIdAndViewType(Long projectBoardId,
                                                                         RecruitmentViewType viewType,
                                                                         boolean isAlreadyApplied) {
        ProjectBoardDetails projectBoardDetails = queryFactory
                .select(getBoardDetailsExpression(viewType, isAlreadyApplied))
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

    private ConstructorExpression<ProjectBoardDetails> getBoardDetailsExpression(RecruitmentViewType viewType,
                                                                                 boolean isAlreadyApplied) {
        return Projections.constructor(ProjectBoardDetails.class,
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
                projectApply.id.member.count().intValue(),
                Expressions.constant(viewType),
                Expressions.constant(isAlreadyApplied));
    }

    /**
     * 학과에 관계없이 모집중인 상태의 모든 프로젝트 모집 게시판을 페이지 단위로 조회합니다.
     *
     * @param pageable 페이지 정보
     * @return 프로젝트 모집 게시판 목록
     */
    @Override
    public List<ProjectBoardOverview> findProjectBoardOverviewsByPage(Pageable pageable) {
        return queryFactory
                .select(getBoardOverviewExpression())
                .from(projectBoard)
                .leftJoin(projectApply)
                .on(hasMatchingProjectBoardId(projectApply.id.projectBoard.id))
                .leftJoin(projectBoardDepartment)
                .on(hasMatchingProjectBoardId(projectBoardDepartment.id.projectBoard.id))
                .where(isRecruiting())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(projectBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), projectBoard))
                .fetch();
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

    private BooleanExpression isRecruiting() {
        return projectBoard.endAt.gt(LocalDateTime.now())
                .and(projectBoard.startAt.lt(LocalDateTime.now()));
    }
}
