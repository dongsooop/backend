package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitment;
import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringApply;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringBoard;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
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

    public List<TutoringBoardOverview> findTutoringBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                         Pageable pageable) {
        return queryFactory.select(Projections.constructor(TutoringBoardOverview.class,
                        tutoringBoard.id,
                        tutoringApply.id.member.count().intValue(),
                        tutoringBoard.startAt,
                        tutoringBoard.endAt,
                        tutoringBoard.title,
                        tutoringBoard.content,
                        tutoringBoard.tags,
                        tutoringBoard.department.id))
                .from(tutoringBoard)
                .leftJoin(tutoringApply)
                .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id))
                .where(tutoringBoard.department.id.eq(departmentType))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(tutoringBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), tutoringBoard))
                .fetch();
    }

    public Optional<TutoringBoardDetails> findBoardDetailsByIdAndViewType(Long tutoringBoardId,
                                                                          RecruitmentViewType viewType,
                                                                          boolean isAlreadyApplied) {
        return Optional.ofNullable(
                queryFactory.select(Projections.constructor(TutoringBoardDetails.class,
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
                                Expressions.constant(isAlreadyApplied)))
                        .from(tutoringBoard)
                        .leftJoin(tutoringApply)
                        .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id))
                        .where(tutoringBoard.id.eq(tutoringBoardId))
                        .groupBy(tutoringBoard.id, tutoringBoard.author.nickname)
                        .fetchOne());
    }

    public List<TutoringBoardOverview> findTutoringBoardOverviewsByPage(Pageable pageable) {
        return queryFactory.select(Projections.constructor(TutoringBoardOverview.class,
                        tutoringBoard.id,
                        tutoringApply.id.member.count().intValue(),
                        tutoringBoard.startAt,
                        tutoringBoard.endAt,
                        tutoringBoard.title,
                        tutoringBoard.content,
                        tutoringBoard.tags,
                        tutoringBoard.department.id))
                .from(tutoringBoard)
                .leftJoin(tutoringApply)
                .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id))
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

    /**
     * 특정 회원이 신청한 튜터링 모집 게시판 목록을 페이지 단위로 조회합니다.
     *
     * @param memberId 회원 ID
     * @param pageable 페이지 정보
     * @return 신청한 튜터링 모집 게시판 목록
     */
    @Override
    public List<ApplyRecruitment> findApplyRecruitmentsByMemberId(Long memberId, Pageable pageable) {
        return queryFactory
                .select(getApplyRecruitmentExpression())
                .from(tutoringBoard)
                .leftJoin(tutoringApply)
                .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id)
                        .and(tutoringApply.id.member.id.eq(memberId)))
                .where(tutoringApply.id.member.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(tutoringBoard.id, tutoringApply.applyTime)
                .orderBy(tutoringApply.applyTime.desc())
                .fetch();
    }

    private ConstructorExpression<ApplyRecruitment> getApplyRecruitmentExpression() {
        return Projections.constructor(ApplyRecruitment.class,
                tutoringBoard.id,
                tutoringApply.id.member.countDistinct().intValue(),
                tutoringBoard.startAt,
                tutoringBoard.endAt,
                tutoringBoard.title,
                tutoringBoard.content,
                tutoringBoard.tags,
                tutoringBoard.department.id.stringValue(),
                Expressions.constant(RecruitmentType.TUTORING),
                tutoringBoard.createdAt,
                isRecruiting());
    }

    @Override
    public List<OpenedRecruitment> findOpenedRecruitmentsByMemberId(Long memberId, Pageable pageable) {
        return queryFactory
                .select(getOpenedRecruitmentExpression())
                .from(tutoringBoard)
                .leftJoin(tutoringApply)
                .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id)
                        .and(tutoringApply.id.member.id.eq(memberId)))
                .where(tutoringApply.id.member.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(tutoringBoard.id)
                .orderBy(tutoringBoard.createdAt.desc())
                .fetch();
    }

    private ConstructorExpression<OpenedRecruitment> getOpenedRecruitmentExpression() {
        return Projections.constructor(OpenedRecruitment.class,
                tutoringBoard.id,
                tutoringApply.id.member.countDistinct().intValue(),
                tutoringBoard.startAt,
                tutoringBoard.endAt,
                tutoringBoard.title,
                tutoringBoard.content,
                tutoringBoard.tags,
                tutoringBoard.department.id.stringValue(),
                Expressions.constant(RecruitmentType.TUTORING),
                tutoringBoard.createdAt,
                isRecruiting());
    }

    private BooleanExpression isRecruiting() {
        return tutoringBoard.endAt.gt(LocalDateTime.now())
                .and(tutoringBoard.startAt.lt(LocalDateTime.now()));
    }
}
