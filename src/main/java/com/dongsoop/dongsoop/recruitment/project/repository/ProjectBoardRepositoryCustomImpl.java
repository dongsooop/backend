package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitment;
import com.dongsoop.dongsoop.recruitment.RecruitmentType;
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
                .having(equalDepartmentType(departmentType))
                .fetch();
    }

    public Optional<ProjectBoardDetails> findBoardDetailsByIdAndViewType(Long projectBoardId,
                                                                         RecruitmentViewType viewType,
                                                                         boolean isAlreadyApplied) {
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
                        projectApply.id.member.count().intValue(),
                        Expressions.constant(viewType),
                        Expressions.constant(isAlreadyApplied)))
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

    private BooleanExpression equalDepartmentType(DepartmentType departmentType) {
        return Expressions.numberTemplate(
                        Integer.class,
                        "SUM(CASE WHEN {0} = {1} THEN 1 ELSE 0 END)",
                        projectBoardDepartment.id.department.id,
                        Expressions.constant(departmentType))
                .gt(0);
    }

    /**
     * 특정 회원이 신청한 프로젝트 모집 게시판 목록을 페이지 단위로 조회합니다.
     *
     * @param memberId 회원 ID
     * @param pageable 페이지 정보
     * @return 신청한 프로젝트 모집 게시판 목록
     */
    @Override
    public List<ApplyRecruitment> findApplyRecruitmentsByMemberId(Long memberId, Pageable pageable) {
        return queryFactory
                .select(getApplyRecruitmentExpression())
                .from(projectBoard)
                .leftJoin(projectApply)
                .on(projectApply.id.projectBoard.id.eq(projectBoard.id)
                        .and(projectApply.id.member.id.eq(memberId)))
                .leftJoin(projectBoardDepartment)
                .on(hasMatchingProjectBoardId(projectBoardDepartment.id.projectBoard.id))
                .where(projectApply.id.member.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(projectBoard.id, projectApply.applyTime)
                .orderBy(projectApply.applyTime.desc())
                .fetch();
    }

    private ConstructorExpression<ApplyRecruitment> getApplyRecruitmentExpression() {
        return Projections.constructor(ApplyRecruitment.class,
                projectBoard.id,
                projectApply.id.member.countDistinct().intValue(),
                projectBoard.startAt,
                projectBoard.endAt,
                projectBoard.title,
                projectBoard.content,
                projectBoard.tags,
                Expressions.stringTemplate("string_agg({0}, ',')",
                        projectBoardDepartment.id.department.id),
                Expressions.constant(RecruitmentType.PROJECT),
                projectBoard.createdAt,
                isRecruiting());
    }

    @Override
    public List<OpenedRecruitment> findOpenedRecruitmentsByMemberId(Long memberId, Pageable pageable) {
        return queryFactory
                .select(getOpenedRecruitmentExpression())
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

    private ConstructorExpression<OpenedRecruitment> getOpenedRecruitmentExpression() {
        return Projections.constructor(OpenedRecruitment.class,
                projectBoard.id,
                projectApply.id.member.countDistinct().intValue(),
                projectBoard.startAt,
                projectBoard.endAt,
                projectBoard.title,
                projectBoard.content,
                projectBoard.tags,
                Expressions.stringTemplate("string_agg({0}, ',')",
                        projectBoardDepartment.id.department.id),
                Expressions.constant(RecruitmentType.PROJECT),
                projectBoard.createdAt,
                isRecruiting());
    }

    private BooleanExpression isRecruiting() {
        return projectBoard.endAt.gt(LocalDateTime.now())
                .and(projectBoard.startAt.lt(LocalDateTime.now()));
    }
}
