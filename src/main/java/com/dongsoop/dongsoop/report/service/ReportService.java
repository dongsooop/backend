package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.report.dto.CreateBlindDateReportRequest;
import com.dongsoop.dongsoop.report.dto.CreateChatReportRequest;
import com.dongsoop.dongsoop.report.dto.CreateReportRequest;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.dto.ReportContextResponse;
import com.dongsoop.dongsoop.report.dto.SanctionStatusResponse;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReportService {

    void createReport(CreateReportRequest request);

    void createChatReport(CreateChatReportRequest request);

    void createBlindDateReport(CreateBlindDateReportRequest request);

    void processSanction(ProcessSanctionRequest request);

    void dismissReport(Long reportId);

    List<?> getReports(ReportFilterType filterType, Pageable pageable);

    ReportContextResponse getReportContext(Long reportId);

    SanctionStatusResponse checkAndUpdateSanctionStatus();
}