package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.dto.ReportSummaryResponse;
import com.dongsoop.dongsoop.report.entity.QReport;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
    public Page<ReportResponse> findAllReportsWithDetails(Pageable pageable) {
        List<ReportResponse> reports = queryFactory
                .select(Projections.constructor(ReportResponse.class,
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
                        report.createdAt))
                .from(report)
                .leftJoin(report.reporter, reporter)
                .leftJoin(report.admin, admin)
                .leftJoin(report.targetMember, targetMember)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), report))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = Optional.ofNullable(queryFactory
                        .select(report.count())
                        .from(report)
                        .fetchOne())
                .orElse(0L);

        return new PageImpl<>(reports, pageable, total);
    }

    @Override
    public Page<ReportSummaryResponse> findUnprocessedReports(Pageable pageable) {
        List<ReportSummaryResponse> reports = queryFactory
                .select(Projections.constructor(ReportSummaryResponse.class,
                        report.id,
                        reporter.nickname,
                        report.reportType,
                        report.reportReason,
                        report.isProcessed,
                        report.createdAt))
                .from(report)
                .leftJoin(report.reporter, reporter)
                .where(isUnprocessed())
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), report))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = Optional.ofNullable(queryFactory
                        .select(report.count())
                        .from(report)
                        .where(isUnprocessed())
                        .fetchOne())
                .orElse(0L);

        return new PageImpl<>(reports, pageable, total);
    }

    @Override
    public Page<ReportResponse> findProcessedReports(Pageable pageable) {
        List<ReportResponse> reports = queryFactory
                .select(Projections.constructor(ReportResponse.class,
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
                        report.createdAt))
                .from(report)
                .leftJoin(report.reporter, reporter)
                .leftJoin(report.admin, admin)
                .leftJoin(report.targetMember, targetMember)
                .where(isProcessed())
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), report))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = Optional.ofNullable(queryFactory
                        .select(report.count())
                        .from(report)
                        .where(isProcessed())
                        .fetchOne())
                .orElse(0L);

        return new PageImpl<>(reports, pageable, total);
    }

    @Override
    public Page<ReportResponse> findActiveSanctions(Pageable pageable) {
        List<ReportResponse> reports = queryFactory
                .select(Projections.constructor(ReportResponse.class,
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
                        report.createdAt))
                .from(report)
                .leftJoin(report.reporter, reporter)
                .leftJoin(report.admin, admin)
                .leftJoin(report.targetMember, targetMember)
                .where(isSanctionActive())
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), report))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = Optional.ofNullable(queryFactory
                        .select(report.count())
                        .from(report)
                        .where(isSanctionActive())
                        .fetchOne())
                .orElse(0L);

        return new PageImpl<>(reports, pageable, total);
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