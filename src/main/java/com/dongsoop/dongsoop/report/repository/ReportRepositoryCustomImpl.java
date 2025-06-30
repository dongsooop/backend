package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.entity.QReport;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.entity.SanctionType;
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

@Repository
@RequiredArgsConstructor
public class ReportRepositoryCustomImpl implements ReportRepositoryCustom {

    private static final QReport report = QReport.report;
    private static final QMember reporter = new QMember("reporter");
    private static final QMember admin = new QMember("admin");
    private static final QMember targetMember = new QMember("targetMember");

    private final JPAQueryFactory queryFactory;
    private final PageableUtil pageableUtil;

    @Override
    public List<ReportResponse> findDetailedReportsByFilter(ReportFilterType filterType, Pageable pageable) {
        BooleanExpression filterCondition = createFilterCondition(filterType);
        Expression<ReportResponse> projection = createDetailedProjection();

        return buildQuery(filterCondition, projection, pageable);
    }

    @Override
    public List<ReportResponse> findSummaryReportsByFilter(ReportFilterType filterType, Pageable pageable) {
        BooleanExpression filterCondition = createFilterCondition(filterType);
        Expression<ReportResponse> projection = createSummaryProjection();

        return buildQuery(filterCondition, projection, pageable);
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
                report.sanctionType,
                report.sanctionReason,
                report.sanctionStartAt,
                report.sanctionEndAt,
                report.isSanctionActive,
                report.createdAt);
    }

    private Expression<ReportResponse> createSummaryProjection() {
        return Projections.constructor(ReportResponse.class,
                report.id,
                reporter.nickname,
                report.reportType,
                Expressions.nullExpression(Long.class),
                Expressions.nullExpression(String.class),
                report.reportReason,
                Expressions.nullExpression(String.class),
                report.isProcessed,
                Expressions.nullExpression(String.class),
                Expressions.nullExpression(String.class),
                Expressions.nullExpression(SanctionType.class),
                Expressions.nullExpression(String.class),
                Expressions.nullExpression(LocalDateTime.class),
                Expressions.nullExpression(LocalDateTime.class),
                Expressions.nullExpression(Boolean.class),
                report.createdAt);
    }

    private JPAQuery<ReportResponse> createBaseQuery(Expression<ReportResponse> projection,
                                                     BooleanExpression filterCondition) {
        return queryFactory
                .select(projection)
                .from(report)
                .leftJoin(report.reporter, reporter)
                .leftJoin(report.admin, admin)
                .leftJoin(report.targetMember, targetMember)
                .where(filterCondition);
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
        return report.isSanctionActive.eq(true);
    }
}