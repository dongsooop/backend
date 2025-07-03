package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.exception.domain.member.MemberNotFoundException;
import com.dongsoop.dongsoop.exception.domain.report.ReportNotFoundException;
import com.dongsoop.dongsoop.exception.domain.report.SanctionAlreadyExistsException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.CreateReportRequest;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.util.ReportUrlGenerator;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final ReportValidator reportValidator;
    private final ReportUrlGenerator urlGenerator;
    private final SanctionExecutor sanctionExecutor;

    @Override
    @Transactional
    public void createReport(CreateReportRequest request) {
        Member reporter = memberService.getMemberReferenceByContext();
        reportValidator.validateAll(reporter, request.reportType(), request.targetId());

        String targetUrl = urlGenerator.generateUrl(request.reportType(), request.targetId());
        Report report = buildReport(request, reporter, targetUrl);
        reportRepository.save(report);
    }

    @Override
    @Transactional
    public void processSanction(ProcessSanctionRequest request) {
        Report report = findReportById(request.reportId());
        checkReportNotProcessed(report);

        Member targetMember = findMemberById(request.targetMemberId());
        Member admin = memberService.getMemberReferenceByContext();

        processSanctionForReport(report, request, admin, targetMember);
        sanctionExecutor.executeSanction(report);
    }

    @Override
    public List<?> getReports(ReportFilterType filterType, Pageable pageable) {
        if (ReportFilterType.UNPROCESSED.equals(filterType)) {
            return reportRepository.findSummaryReportsByFilter(filterType, pageable);
        }
        return reportRepository.findDetailedReportsByFilter(filterType, pageable);
    }

    private Report buildReport(CreateReportRequest request, Member reporter, String targetUrl) {
        return Report.builder()
                .reporter(reporter)
                .reportType(request.reportType())
                .targetId(request.targetId())
                .reportReason(request.reason())
                .description(request.description())
                .targetUrl(targetUrl)
                .build();
    }

    private void processSanctionForReport(Report report, ProcessSanctionRequest request, Member admin, Member targetMember) {
        report.processSanction(admin, targetMember, request.sanctionType(),
                request.sanctionReason(), request.sanctionEndAt());
    }

    private void checkReportNotProcessed(Report report) {
        if (report.getIsProcessed()) {
            throw new SanctionAlreadyExistsException(report.getId());
        }
    }

    private Report findReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }
}