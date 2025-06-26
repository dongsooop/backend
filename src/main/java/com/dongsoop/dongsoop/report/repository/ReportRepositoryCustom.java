package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.dto.ReportSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportRepositoryCustom {

    Page<ReportResponse> findAllReportsWithDetails(Pageable pageable);

    Page<ReportSummaryResponse> findUnprocessedReports(Pageable pageable);

    Page<ReportResponse> findProcessedReports(Pageable pageable);

    Page<ReportResponse> findActiveSanctions(Pageable pageable);
}