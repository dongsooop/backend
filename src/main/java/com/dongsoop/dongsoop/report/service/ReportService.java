package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.report.dto.CreateReportRequest;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    void createReport(CreateReportRequest request);

    void processSanction(ProcessSanctionRequest request);
    
    Page<ReportResponse> getReports(ReportFilterType filterType, Pageable pageable);
}
