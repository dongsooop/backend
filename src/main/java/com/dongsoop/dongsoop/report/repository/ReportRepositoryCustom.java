package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReportRepositoryCustom {

    List<ReportResponse> findDetailedReportsByFilter(ReportFilterType filterType, Pageable pageable);

    List<ReportResponse> findSummaryReportsByFilter(ReportFilterType filterType, Pageable pageable);
}