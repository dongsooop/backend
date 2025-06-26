package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.exception.domain.member.MemberNotFoundException;
import com.dongsoop.dongsoop.exception.domain.report.MemberSanctionedException;
import com.dongsoop.dongsoop.exception.domain.report.ReportNotFoundException;
import com.dongsoop.dongsoop.exception.domain.report.SanctionAlreadyExistsException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.CreateReportRequest;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.dto.ReportSummaryResponse;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.util.ReportUrlGenerator;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final ReportValidator reportValidator;
    private final ReportUrlGenerator urlGenerator;
    private final SanctionExecutor sanctionExecutor;

    @Transactional
    public void createReport(CreateReportRequest request) {
        Member reporter = memberService.getMemberReferenceByContext();
        reportValidator.validateAll(reporter, request.reportType(), request.targetId());

        String targetUrl = urlGenerator.generateUrl(request.reportType(), request.targetId());
        Report report = buildReport(request, reporter, targetUrl);
        reportRepository.save(report);
    }

    @Transactional
    public void processSanction(ProcessSanctionRequest request) {
        Report report = findReportById(request.reportId());
        checkReportNotProcessed(report);

        Member targetMember = findMemberById(request.targetMemberId());
        Member admin = memberService.getMemberReferenceByContext();

        processSanctionForReport(report, request, admin, targetMember);
        sanctionExecutor.executeSanction(report);
    }

    public void checkMemberAccess(Member member) {
        reportRepository.findActiveBanForMember(member.getId(), LocalDateTime.now())
                .ifPresent(report -> {
                    throw new MemberSanctionedException("회원님은 현재 제재 중입니다. 자세한 내용은 고객센터에 문의해주세요.");
                });
    }

    public void checkMemberAccessById(Long memberId) {
        reportRepository.findActiveBanForMember(memberId, LocalDateTime.now())
                .ifPresent(report -> {
                    throw new MemberSanctionedException("회원님은 현재 제재 중입니다. 자세한 내용은 고객센터에 문의해주세요.");
                });
    }

    public Page<ReportResponse> getAllReports(Pageable pageable) {
        return reportRepository.findAllReportsWithDetails(pageable);
    }

    public Page<ReportSummaryResponse> getUnprocessedReports(Pageable pageable) {
        return reportRepository.findUnprocessedReports(pageable);
    }

    public Page<ReportResponse> getProcessedReports(Pageable pageable) {
        return reportRepository.findProcessedReports(pageable);
    }

    public Page<ReportResponse> getActiveSanctions(Pageable pageable) {
        return reportRepository.findActiveSanctions(pageable);
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
        Optional.of(report.getIsProcessed())
                .filter(processed -> processed)
                .ifPresent(processed -> {
                    throw new SanctionAlreadyExistsException(report.getId());
                });
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