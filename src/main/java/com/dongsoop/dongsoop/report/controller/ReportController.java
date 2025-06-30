package com.dongsoop.dongsoop.report.controller;

import com.dongsoop.dongsoop.report.dto.CreateReportRequest;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.service.ReportService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> createReport(@RequestBody @Valid CreateReportRequest request) {
        reportService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/sanctions")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Void> processSanction(@RequestBody @Valid ProcessSanctionRequest request) {
        reportService.processSanction(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/admin")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Page<ReportResponse>> getReports(
            @RequestParam(defaultValue = "ALL") ReportFilterType filter,
            Pageable pageable) {
        Page<ReportResponse> reports = reportService.getReports(filter, pageable);
        return ResponseEntity.ok(reports);
    }
}