package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.dto.ReportSummaryResponse;
import com.dongsoop.dongsoop.report.entity.*;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryCustomImpl implements ReportRepositoryCustom {

    private static final QReport report = QReport.report;
    private static final QMember reporter = new QMember("reporter");
    private static final QMember admin = new QMember("admin");
    private static final QMember targetMember = new QMember("targetMember");
    private static final QSanction sanction = QSanction.sanction;

    private final JPAQueryFactory queryFactory;
    private final PageableUtil pageableUtil;

    @Override
    public List<ReportResponse> findDetailedReportsByFilter(ReportFilterType filterType, Pageable pageable) {
        BooleanExpression filterCondition = createFilterCondition(filterType);
        Expression<ReportResponse> projection = createDetailedProjection();

        return buildQuery(filterCondition, projection, pageable);
    }

    @Override
    public List<ReportSummaryResponse> findSummaryReportsByFilter(ReportFilterType filterType, Pageable pageable) {
        BooleanExpression filterCondition = createFilterCondition(filterType);
        Expression<ReportSummaryResponse> projection = createSummaryProjection();

        return buildSummaryQuery(filterCondition, projection, pageable);
    }

    private List<ReportSummaryResponse> buildSummaryQuery(BooleanExpression filterCondition,
                                                          Expression<ReportSummaryResponse> projection, Pageable pageable) {
        JPAQuery<ReportSummaryResponse> baseQuery = createSummaryBaseQuery(projection, filterCondition);
        return applySummaryPaginationAndSorting(baseQuery, pageable);
    }

    private List<ReportSummaryResponse> applySummaryPaginationAndSorting(JPAQuery<ReportSummaryResponse> query, Pageable pageable) {
        return query
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), report))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private JPAQuery<ReportSummaryResponse> createSummaryBaseQuery(Expression<ReportSummaryResponse> projection,
                                                                   BooleanExpression filterCondition) {
        return queryFactory
                .select(projection)
                .from(report)
                .leftJoin(report.reporter, reporter)
                .leftJoin(report.targetMember, targetMember)
                .where(filterCondition);
    }

    private List<ReportResponse> buildQuery(BooleanExpression filterCondition,
                                            Expression<ReportResponse> projection, Pageable pageable) {
        JPAQuery<ReportResponse> baseQuery = createBaseQuery(projection, filterCondition);
        return applyPaginationAndSorting(baseQuery, pageable);
    }

    private Expression<ReportResponse> createDetailedProjection() {
        return Projections.constructor(ReportResponse.class,
                report.id,
                reporter.nickname,
                report.reportType,
                report.targetId,
                report.targetUrl,
                report.reportReason,
                report.description,
                report.isProcessed,
                admin.nickname,
                targetMember.nickname,
                sanction.sanctionType,
                sanction.reason,
                sanction.startDate,
                sanction.endDate,
                sanction.isActive,
                report.createdAt);
    }

    private Expression<ReportSummaryResponse> createSummaryProjection() {
        return Projections.constructor(ReportSummaryResponse.class,
                report.id,
                reporter.nickname,
                report.reportType,
                report.reportReason,
                report.isProcessed,
                report.createdAt,
                report.targetMember.id,
                report.description
        );
    }


    private JPAQuery<ReportResponse> createBaseQuery(Expression<ReportResponse> projection,
                                                     BooleanExpression filterCondition) {
        return queryFactory
                .select(projection)
                .from(report)
                .leftJoin(report.reporter, reporter)
                .leftJoin(report.admin, admin)
                .leftJoin(report.targetMember, targetMember)
                .leftJoin(report.sanction, sanction)
                .where(filterCondition);
    }

    @Override
    public Optional<Report> findActiveBanForMember(Long memberId, LocalDateTime currentTime, List<SanctionType> sanctionTypes) {
        BooleanExpression condition = report.targetMember.id.eq(memberId)
                .and(sanction.isActive.eq(true))
                .and(sanction.sanctionType.in(sanctionTypes))
                .and(sanction.endDate.isNull().or(sanction.endDate.gt(currentTime)));

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(report)
                        .leftJoin(report.sanction, sanction)
                        .where(condition)
                        .orderBy(report.createdAt.desc())
                        .fetchFirst()
        );
    }

    private List<ReportResponse> applyPaginationAndSorting(JPAQuery<ReportResponse> query, Pageable pageable) {
        return query
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), report))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private BooleanExpression createFilterCondition(ReportFilterType filterType) {
        if (ReportFilterType.UNPROCESSED.equals(filterType)) {
            return isUnprocessed();
        }

        if (ReportFilterType.PROCESSED.equals(filterType)) {
            return isProcessed();
        }

        if (ReportFilterType.ACTIVE_SANCTIONS.equals(filterType)) {
            return isSanctionActive();
        }

        return Expressions.TRUE;
    }

    private BooleanExpression isUnprocessed() {
        return report.isProcessed.eq(false);
    }

    private BooleanExpression isProcessed() {
        return report.isProcessed.eq(true);
    }

    private BooleanExpression isSanctionActive() {
        return sanction.isActive.eq(true).and(report.sanction.isNotNull());
    }
}