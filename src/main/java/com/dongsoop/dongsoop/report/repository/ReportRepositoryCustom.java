package com.dongsoop.dongsoop.report.repository;

import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.dto.ReportSummaryResponse;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepositoryCustom {

    List<ReportResponse> findDetailedReportsByFilter(ReportFilterType filterType, Pageable pageable);

    List<ReportSummaryResponse> findSummaryReportsByFilter(ReportFilterType filterType, Pageable pageable);

    Optional<Report> findActiveBanForMember(Long memberId, LocalDateTime currentTime, List<SanctionType> sanctionTypes);

    List<Report> findUnprocessedReports(Pageable pageable);
}