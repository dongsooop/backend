package com.dongsoop.dongsoop.recruitment.board.project.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.memberblock.annotation.ApplyBlockFilter;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.apply.project.entity.QProjectApply;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.project.entity.QProjectBoard;
import com.dongsoop.dongsoop.recruitment.board.project.entity.QProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.projection.ProjectRecruitmentProjection;
import com.dongsoop.dongsoop.recruitment.repository.RecruitmentRepositoryUtils;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
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

    private final RecruitmentRepositoryUtils recruitmentRepositoryUtils;

    private final ProjectRecruitmentProjection projection;

    /**
     * 학과별로 모집중인 상태의 프로젝트 모집 게시판 목록을 페이지 단위로 조회합니다.
     *
     * @param departmentType 학과 타입
     * @param pageable       페이지 정보
     * @return 모집중인 프로젝트 모집 게시판 목록
     */
    @Override
    @ApplyBlockFilter
    public List<RecruitmentOverview> findProjectBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                      Pageable pageable) {
        return queryFactory
                .select(projection.getRecruitmentOverviewExpression())
                .from(projectBoard)
                .leftJoin(projectApply)
                .on(hasMatchingProjectBoardId(projectApply.id.projectBoard.id))
                .leftJoin(projectBoardDepartment)
                .on(hasMatchingProjectBoardId(projectBoardDepartment.id.projectBoard.id))
                .where(recruitmentRepositoryUtils.isRecruiting(projectBoard.startAt, projectBoard.endAt)
                        .and(projectBoard.id.in(includeDepartmentType(departmentType))))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(projectBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), projectBoard))
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
    public Optional<RecruitmentDetails> findBoardDetailsByIdAndViewType(Long projectBoardId,
                                                                        RecruitmentViewType viewType,
                                                                        boolean isAlreadyApplied) {
        RecruitmentDetails details = queryFactory
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

        return Optional.ofNullable(details);
    }

    /**
     * 학과에 관계없이 모집중인 상태의 모든 프로젝트 모집 게시판을 페이지 단위로 조회합니다.
     *
     * @param pageable 페이지 정보
     * @return 프로젝트 모집 게시판 목록
     */
    @Override
    @ApplyBlockFilter
    public List<RecruitmentOverview> findProjectBoardOverviewsByPage(Pageable pageable) {
        return queryFactory
                .select(projection.getRecruitmentOverviewExpression())
                .from(projectBoard)
                .leftJoin(projectApply)
                .on(hasMatchingProjectBoardId(projectApply.id.projectBoard.id))
                .leftJoin(projectBoardDepartment)
                .on(hasMatchingProjectBoardId(projectBoardDepartment.id.projectBoard.id))
                .where(recruitmentRepositoryUtils.isRecruiting(projectBoard.startAt, projectBoard.endAt))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(projectBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), projectBoard))
                .fetch();
    }

    private BooleanExpression hasMatchingProjectBoardId(NumberPath<Long> projectBoardId) {
        return projectBoard.id.eq(projectBoardId);
    }

    private JPQLQuery<Long> includeDepartmentType(DepartmentType departmentType) {
        return JPAExpressions.select(projectBoard.id)
                .leftJoin(projectBoardDepartment)
                .where(projectBoard.id.eq(projectBoardDepartment.id.projectBoard.id)
                        .and(projectBoardDepartment.id.department.id.eq(departmentType)));
    }
}
