package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.CreateReportRequest;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.dto.SanctionStatusResponse;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.exception.ReportNotFoundException;
import com.dongsoop.dongsoop.report.exception.SanctionAlreadyExistsException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.util.ReportUrlGenerator;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final SanctionRepository sanctionRepository;

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

    private void processSanctionForReport(Report report, ProcessSanctionRequest request, Member admin,
                                          Member targetMember) {
        Sanction sanction = createSanction(targetMember, request);
        sanctionRepository.save(sanction);
        report.processSanction(admin, targetMember, sanction);
    }

    private Sanction createSanction(Member targetMember, ProcessSanctionRequest request) {
        return Sanction.builder()
                .member(targetMember)
                .sanctionType(request.sanctionType())
                .reason(request.sanctionReason())
                .startDate(LocalDateTime.now())
                .endDate(request.sanctionEndAt())
                .description(request.sanctionType().getDescription())
                .build();
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

    @Override
    @Transactional
    public SanctionStatusResponse checkAndUpdateSanctionStatus() {
        Long memberId = getMemberId();
        Optional<Sanction> sanctionOpt = findActiveSanction(memberId);

        return sanctionOpt.map(this::processSanctionStatus)
                .orElseGet(() -> createSanctionResponse(false, null));
    }

    private Long getMemberId() {
        return memberService.getMemberIdByAuthentication();
    }

    private Optional<Sanction> findActiveSanction(Long memberId) {
        return sanctionRepository.findActiveSanctionByMemberId(memberId);
    }

    private SanctionStatusResponse processSanctionStatus(Sanction sanction) {
        if (sanction.isCurrentlyExpired()) {
            return handleExpiredSanction(sanction);
        }

        if (sanction.isSanctionActive()) {
            return createSanctionResponse(true, sanction);
        }

        return createSanctionResponse(false, null);
    }

    private SanctionStatusResponse handleExpiredSanction(Sanction sanction) {
        sanction.expireIfNeeded();
        sanctionRepository.save(sanction);
        return SanctionStatusResponse.noSanction();
    }

    private SanctionStatusResponse createSanctionResponse(boolean isSanctioned, Sanction sanction) {
        if (!isSanctioned) {
            return SanctionStatusResponse.noSanction();
        }

        return SanctionStatusResponse.withSanction(sanction);
    }
}