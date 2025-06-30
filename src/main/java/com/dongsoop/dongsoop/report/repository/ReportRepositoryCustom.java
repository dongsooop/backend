package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportRepositoryCustom {

    Page<ReportResponse> findReportsByFilter(ReportFilterType filterType, Pageable pageable);
}