package com.dongsoop.dongsoop.recruitment.board.study.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.memberblock.annotation.ApplyBlockFilter;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.apply.study.entity.QStudyApply;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.projection.StudyRecruitmentProjection;
import com.dongsoop.dongsoop.recruitment.board.study.entity.QStudyBoard;
import com.dongsoop.dongsoop.recruitment.board.study.entity.QStudyBoardDepartment;
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
public class StudyBoardRepositoryCustomImpl implements StudyBoardRepositoryCustom {

    private static final QStudyBoard studyBoard = QStudyBoard.studyBoard;

    private static final QStudyBoardDepartment studyBoardDepartment = QStudyBoardDepartment.studyBoardDepartment;

    private static final QStudyApply studyApply = QStudyApply.studyApply;

    private final JPAQueryFactory queryFactory;

    private final PageableUtil pageableUtil;

    private final StudyRecruitmentProjection projection;

    private final RecruitmentRepositoryUtils recruitmentRepositoryUtils;

    /**
     * 학과별로 모집중인 상태의 스터디 모집 게시판 목록을 페이지 단위로 조회합니다.
     *
     * @param departmentType 학과 타입
     * @param pageable       페이지 정보
     * @return 모집중인 스터디 모집 게시판 목록
     */

    @Override
    @ApplyBlockFilter
    public List<RecruitmentOverview> findStudyBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                    Pageable pageable) {
        return queryFactory
                .select(projection.getRecruitmentOverviewExpression())
                .from(studyBoard)
                .leftJoin(studyApply)
                .on(hasMatchingStudyBoardId(studyApply.id.studyBoard.id))
                .leftJoin(studyBoardDepartment)
                .on(hasMatchingStudyBoardId(studyBoardDepartment.id.studyBoard.id))
                .where(recruitmentRepositoryUtils.isRecruiting(studyBoard.startAt, studyBoard.endAt)
                        .and(studyBoard.id.in(includeDepartmentType(departmentType))))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(studyBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), studyBoard))
                .fetch();
    }

    /**
     * 특정 스터디 모집 게시판 ID를 통해 상세 정보를 조회합니다.
     *
     * @param studyBoardId     스터디 모집 게시판 ID
     * @param viewType         조회자 타입 (예: OWNER, MEMBER, GUEST)
     * @param isAlreadyApplied 현재 멤버가 이미 신청했는지 여부
     * @return 스터디 모집 게시판 상세 정보
     */
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

    /**
     * 학과에 관계없이 모집중인 상태의 모든 스터디 모집 게시판을 페이지 단위로 조회합니다.
     *
     * @param pageable 페이지 정보
     * @return 스터디 모집 게시판 목록
     */
    @Override
    @ApplyBlockFilter
    public List<RecruitmentOverview> findStudyBoardOverviewsByPage(Pageable pageable) {
        return queryFactory
                .select(projection.getRecruitmentOverviewExpression())
                .from(studyBoard)
                .leftJoin(studyApply)
                .on(hasMatchingStudyBoardId(studyApply.id.studyBoard.id))
                .leftJoin(studyBoardDepartment)
                .on(hasMatchingStudyBoardId(studyBoardDepartment.id.studyBoard.id))
                .where(recruitmentRepositoryUtils.isRecruiting(studyBoard.startAt, studyBoard.endAt))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(studyBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), studyBoard))
                .fetch();
    }

    private BooleanExpression hasMatchingStudyBoardId(NumberPath<Long> studyBoardId) {
        return studyBoard.id.eq(studyBoardId);
    }

    private JPQLQuery<Long> includeDepartmentType(DepartmentType departmentType) {
        return JPAExpressions.select(studyBoard.id)
                .leftJoin(studyBoardDepartment)
                .where(studyBoard.id.eq(studyBoardDepartment.id.studyBoard.id)
                        .and(studyBoardDepartment.id.department.id.eq(departmentType)));
    }
}
