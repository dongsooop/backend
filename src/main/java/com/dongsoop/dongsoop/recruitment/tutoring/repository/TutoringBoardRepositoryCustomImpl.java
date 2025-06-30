package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.repository.RecruitmentRepositoryUtils;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringApply;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringBoard;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TutoringBoardRepositoryCustomImpl implements TutoringBoardRepositoryCustom {

    private static final QTutoringBoard tutoringBoard = QTutoringBoard.tutoringBoard;

    private static final QTutoringApply tutoringApply = QTutoringApply.tutoringApply;

    private final JPAQueryFactory queryFactory;

    private final PageableUtil pageableUtil;

    private final RecruitmentRepositoryUtils recruitmentRepositoryUtils;

    /**
     * 학과별로 모집중인 상태의 튜터링 모집 게시판 목록을 페이지 단위로 조회합니다.
     *
     * @param departmentType 학과 타입
     * @param pageable       페이지 정보
     * @return 모집중인 튜터링 모집 게시판 목록
     */
    @Override
    public List<TutoringBoardOverview> findTutoringBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                         Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();

        return queryFactory.select(getBoardOverviewExpression())
                .from(tutoringBoard)
                .leftJoin(tutoringApply)
                .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id))
                .where(tutoringBoard.department.id.eq(departmentType)
                        .and(recruitmentRepositoryUtils.isRecruiting(tutoringBoard.startAt, tutoringBoard.endAt, now)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(tutoringBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), tutoringBoard))
                .fetch();
    }

    /**
     * 특정 튜터링 모집 게시판 ID를 통해 상세 정보를 조회합니다.
     *
     * @param tutoringBoardId  튜터링 모집 게시판 ID
     * @param viewType         조회자 타입 (예: OWNER, MEMBER, GUEST)
     * @param isAlreadyApplied 현재 멤버가 이미 신청했는지 여부
     * @return 튜터링 모집 게시판 상세 정보
     */
    @Override
    public Optional<TutoringBoardDetails> findBoardDetailsByIdAndViewType(Long tutoringBoardId,
                                                                          RecruitmentViewType viewType,
                                                                          boolean isAlreadyApplied) {
        return Optional.ofNullable(
                queryFactory.select(getBoardDetailsExpression(viewType, isAlreadyApplied))
                        .from(tutoringBoard)
                        .leftJoin(tutoringApply)
                        .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id))
                        .where(tutoringBoard.id.eq(tutoringBoardId))
                        .groupBy(tutoringBoard.id, tutoringBoard.author.nickname)
                        .fetchOne());
    }

    private ConstructorExpression<TutoringBoardDetails> getBoardDetailsExpression(RecruitmentViewType viewType,
                                                                                  boolean isAlreadyApplied) {
        return Projections.constructor(TutoringBoardDetails.class,
                tutoringBoard.id,
                tutoringBoard.title,
                tutoringBoard.content,
                tutoringBoard.tags,
                tutoringBoard.startAt,
                tutoringBoard.endAt,
                tutoringBoard.department.id,
                tutoringBoard.author.nickname,
                tutoringBoard.createdAt.as("createdAt"),
                tutoringBoard.updatedAt.as("updatedAt"),
                tutoringApply.id.member.count().intValue(),
                Expressions.constant(viewType),
                Expressions.constant(isAlreadyApplied));
    }

    /**
     * 학과에 관계없이 모집중인 상태의 모든 튜터링 모집 게시판을 페이지 단위로 조회합니다.
     *
     * @param pageable 페이지 정보
     * @return 튜터링 모집 게시판 목록
     */
    @Override
    public List<TutoringBoardOverview> findTutoringBoardOverviewsByPage(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();

        return queryFactory.select(getBoardOverviewExpression())
                .from(tutoringBoard)
                .leftJoin(tutoringApply)
                .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id))
                .where(recruitmentRepositoryUtils.isRecruiting(tutoringBoard.startAt, tutoringBoard.endAt, now))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(tutoringBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), tutoringBoard))
                .fetch();
    }

    private ConstructorExpression<TutoringBoardOverview> getBoardOverviewExpression() {
        return Projections.constructor(TutoringBoardOverview.class,
                tutoringBoard.id,
                tutoringApply.id.member.count().intValue(),
                tutoringBoard.startAt,
                tutoringBoard.endAt,
                tutoringBoard.title,
                tutoringBoard.content,
                tutoringBoard.tags,
                tutoringBoard.department.id);
    }
}
