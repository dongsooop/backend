package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringApply;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringBoard;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TutoringBoardRepositoryCustomImpl implements TutoringBoardRepositoryCustom {

    private static final QTutoringBoard tutoringBoard = QTutoringBoard.tutoringBoard;

    private static final QTutoringApply tutoringApplication = QTutoringApply.tutoringApply;

    private final JPAQueryFactory queryFactory;

    private final PageableUtil pageableUtil;

    public List<TutoringBoardOverview> findTutoringBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                         Pageable pageable) {
        return queryFactory.select(Projections.constructor(TutoringBoardOverview.class,
                        tutoringBoard.id,
                        tutoringApplication.id.member.count().intValue(),
                        tutoringBoard.startAt,
                        tutoringBoard.endAt,
                        tutoringBoard.title,
                        tutoringBoard.content,
                        tutoringBoard.tags,
                        tutoringBoard.department.id))
                .from(tutoringBoard)
                .leftJoin(tutoringApplication)
                .on(tutoringApplication.id.tutoringBoard.id.eq(tutoringBoard.id))
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
                                tutoringApplication.id.member.count().intValue(),
                                Expressions.constant(viewType),
                                Expressions.constant(isAlreadyApplied)))
                        .from(tutoringBoard)
                        .leftJoin(tutoringApplication)
                        .on(tutoringApplication.id.tutoringBoard.id.eq(tutoringBoard.id))
                        .where(tutoringBoard.id.eq(tutoringBoardId))
                        .groupBy(tutoringBoard.id, tutoringBoard.author.nickname)
                        .fetchOne());
    }

    public List<TutoringBoardOverview> findTutoringBoardOverviewsByPage(Pageable pageable) {
        return queryFactory.select(Projections.constructor(TutoringBoardOverview.class,
                        tutoringBoard.id,
                        tutoringApplication.id.member.count().intValue(),
                        tutoringBoard.startAt,
                        tutoringBoard.endAt,
                        tutoringBoard.title,
                        tutoringBoard.content,
                        tutoringBoard.tags,
                        tutoringBoard.department.id))
                .from(tutoringBoard)
                .leftJoin(tutoringApplication)
                .on(tutoringApplication.id.tutoringBoard.id.eq(tutoringBoard.id))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(tutoringBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), tutoringBoard))
                .fetch();
    }
}
