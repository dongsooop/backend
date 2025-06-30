package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.entity.QReport;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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
    public Page<ReportResponse> findReportsByFilter(ReportFilterType filterType, Pageable pageable) {
        BooleanExpression filterCondition = createFilterCondition(filterType);
        boolean isDetailedView = requiresDetailedView(filterType);

        List<ReportResponse> reports = buildQuery(filterCondition, isDetailedView, pageable);
        Long total = countByFilter(filterCondition);

        return new PageImpl<>(reports, pageable, total);
    }

    private List<ReportResponse> buildQuery(BooleanExpression filterCondition,
                                            boolean isDetailedView, Pageable pageable) {
        Expression<ReportResponse> projection = createProjection(isDetailedView);
        JPAQuery<ReportResponse> baseQuery = createBaseQuery(projection, filterCondition);

        return applyPaginationAndSorting(baseQuery, pageable);
    }

    private Expression<ReportResponse> createProjection(boolean isDetailedView) {
        if (isDetailedView) {
            return createDetailedProjection();
        }
        return createSummaryProjection();
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
                report.reportReason,
                report.isProcessed,
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

    private Long countByFilter(BooleanExpression filterCondition) {
        return queryFactory
                .select(report.count())
                .from(report)
                .where(filterCondition)
                .fetchOne();
    }

    private BooleanExpression createFilterCondition(ReportFilterType filterType) {
        String filterTypeName = filterType.name();

        if ("UNPROCESSED".equals(filterTypeName)) {
            return isUnprocessed();
        }

        if ("PROCESSED".equals(filterTypeName)) {
            return isProcessed();
        }

        if ("ACTIVE_SANCTIONS".equals(filterTypeName)) {
            return isSanctionActive();
        }

        return null;
    }

    private boolean requiresDetailedView(ReportFilterType filterType) {
        return filterType != ReportFilterType.UNPROCESSED;
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