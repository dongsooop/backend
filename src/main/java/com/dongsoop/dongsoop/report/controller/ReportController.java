package com.dongsoop.dongsoop.report.controller;

import com.dongsoop.dongsoop.report.dto.CreateBlindDateReportRequest;
import com.dongsoop.dongsoop.report.dto.CreateChatReportRequest;
import com.dongsoop.dongsoop.report.dto.CreateReportRequest;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.dto.SanctionStatusResponse;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.service.ReportService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/chat")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> createChatReport(@RequestBody @Valid CreateChatReportRequest request) {
        reportService.createChatReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/blinddate")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> createBlindDateReport(@RequestBody @Valid CreateBlindDateReportRequest request) {
        reportService.createBlindDateReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/sanction-status")
    public ResponseEntity<SanctionStatusResponse> checkSanctionStatus() {
        SanctionStatusResponse response = reportService.checkAndUpdateSanctionStatus();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sanctions")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Void> processSanction(@RequestBody @Valid ProcessSanctionRequest request) {
        reportService.processSanction(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{reportId}/dismiss")
    @Secured(RoleType.ADMIN_ROLE)
    public ResponseEntity<Void> dismissReport(@PathVariable("reportId") Long reportId) {
        reportService.dismissReport(reportId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<?>> getReports(
            @RequestParam(defaultValue = "ALL") ReportFilterType filter,
            Pageable pageable) {
        List<?> reports = reportService.getReports(filter, pageable);
        return ResponseEntity.ok(reports);
    }
}