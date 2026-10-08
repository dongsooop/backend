package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.chat.exception.GroupChatOnlyException;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.board.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.report.dto.CreateBlindDateReportRequest;
import com.dongsoop.dongsoop.report.dto.CreateChatReportRequest;
import com.dongsoop.dongsoop.report.dto.CreateReportRequest;
import com.dongsoop.dongsoop.report.dto.MessageReportDraft;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.dto.ReportContextResponse;
import com.dongsoop.dongsoop.report.dto.ReportContextResponse.AfterSource;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.dto.SanctionStatusResponse;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.exception.DuplicateReportException;
import com.dongsoop.dongsoop.report.exception.ReportNotFoundException;
import com.dongsoop.dongsoop.report.exception.ReportTargetNotFoundException;
import com.dongsoop.dongsoop.report.exception.SanctionTargetMismatchException;
import com.dongsoop.dongsoop.report.exception.UnsupportedReportTypeException;
import com.dongsoop.dongsoop.report.exception.UnsupportedSanctionTypeException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.util.ReportUrlGenerator;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    // 사전 중복 조회와 저장 사이의 경합으로 신고 유니크 인덱스에 걸린 경우만 중복 신고로 바꾼다
    private static final Set<String> DUPLICATE_REPORT_CONSTRAINT_NAMES =
            Set.of("uk_report_reporter_message", "uk_report_blinddate_reporter_target");

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final ReportValidator reportValidator;
    private final ReportUrlGenerator urlGenerator;
    private final SanctionExecutor sanctionExecutor;
    private final SanctionRepository sanctionRepository;
    private final ChatReportTargetResolver chatReportTargetResolver;
    private final BlindDateReportTargetResolver blindDateReportTargetResolver;

    private final ProjectBoardRepository projectBoardRepository;
    private final StudyBoardRepository studyBoardRepository;
    private final MarketplaceBoardRepository marketplaceBoardRepository;
    private final TutoringBoardRepository tutoringBoardRepository;

    @Override
    @Transactional
    public void createReport(CreateReportRequest request) {
        if (request.reportType().isMessageReport()) {
            throw new UnsupportedReportTypeException();
        }

        Member reporter = memberService.getMemberReferenceByContext();
        reportValidator.validateAll(reporter, request.reportType(), request.targetId());

        String targetUrl = urlGenerator.generateUrl(request.reportType(), request.targetId());
        Report report = buildReport(request, reporter, targetUrl);
        reportRepository.save(report);
    }

    @Override
    @Transactional
    public void createChatReport(CreateChatReportRequest request) {
        Long reporterId = memberService.getMemberIdByAuthentication();
        MessageReportDraft draft = chatReportTargetResolver.resolve(reporterId, request);
        createMessageReport(memberRepository.getReferenceById(reporterId), draft, request.reason(),
                request.description());
    }

    @Override
    @Transactional
    public void createBlindDateReport(CreateBlindDateReportRequest request) {
        Long reporterId = memberService.getMemberIdByAuthentication();
        MessageReportDraft draft = blindDateReportTargetResolver.resolve(reporterId, request);
        createMessageReport(memberRepository.getReferenceById(reporterId), draft, request.reason(),
                request.description());
    }

    @Override
    @Transactional
    public void processSanction(ProcessSanctionRequest request) {
        Report report = findReportById(request.reportId());
        report.ensureNotProcessed();

        Member targetMember = findMemberById(request.targetMemberId());
        Member admin = memberService.getMemberReferenceByContext();
        validateSanctionApplicable(report, request);

        sanctionExecutor.issue(report, admin, targetMember, request.sanctionType(), request.sanctionReason(),
                request.sanctionEndAt(), null);
    }

    @Override
    @Transactional
    public void dismissReport(Long reportId) {
        Report report = findReportById(reportId);
        report.dismiss(memberService.getMemberReferenceByContext());
    }

    @Override
    public List<?> getReports(ReportFilterType filterType, Pageable pageable) {
        if (ReportFilterType.UNPROCESSED.equals(filterType)) {
            return reportRepository.findSummaryReportsByFilter(filterType, pageable);
        }
        return reportRepository.findDetailedReportsByFilter(filterType, pageable);
    }

    @Override
    public ReportContextResponse getReportContext(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
        if (!report.getReportType().isMessageReport()) {
            throw new UnsupportedReportTypeException();
        }

        Optional<ChatMessageSnapshots> after;
        AfterSource source = AfterSource.LIVE;
        if (report.getReportType() == ReportType.CHAT_MESSAGE) {
            after = chatReportTargetResolver.findContextAfter(report.getChatRoomId(), report.getMessageId());
        } else if (report.getMessageContextAfter() != null) {
            after = Optional.of(report.getMessageContextAfter());
            source = AfterSource.SAVED;
        } else {
            after = blindDateReportTargetResolver.findLiveContextAfter(report.getChatRoomId(), report.getMessageId());
        }

        return new ReportContextResponse(report.getMessageId(), report.getMessageContent(),
                messagesOf(report.getMessageContext()), messagesOf(after.orElse(null)),
                after.isPresent() ? source : AfterSource.UNAVAILABLE);
    }

    private static List<ChatMessageSnapshot> messagesOf(ChatMessageSnapshots snapshots) {
        return snapshots == null ? List.of() : snapshots.messages();
    }

    private void createMessageReport(Member reporter, MessageReportDraft draft, ReportReason reason,
                                     String description) {
        if (isDuplicateMessageReport(reporter.getId(), draft)) {
            throw new DuplicateReportException();
        }

        saveMessageReport(Report.messageReport(reporter, draft, reason, description));
    }

    private boolean isDuplicateMessageReport(Long reporterId, MessageReportDraft draft) {
        if (draft.reportType() == ReportType.CHAT_MESSAGE) {
            return reportRepository.existsByReporterIdAndMessageId(reporterId, draft.messageId());
        }

        return reportRepository.existsByReporterIdAndReportTypeAndChatRoomIdAndTargetMemberId(
                reporterId, draft.reportType(), draft.chatRoomId(), draft.targetMember().getId());
    }

    private void saveMessageReport(Report report) {
        try {
            reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateReportConstraintViolation(e)) {
                throw new DuplicateReportException();
            }
            throw e;
        }
    }

    private static boolean isDuplicateReportConstraintViolation(DataIntegrityViolationException e) {
        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                return constraintName != null
                        && DUPLICATE_REPORT_CONSTRAINT_NAMES.contains(constraintName.toLowerCase(Locale.ROOT));
            }
        }
        return false;
    }

    private Report buildReport(CreateReportRequest request, Member reporter, String targetUrl) {
        Member targetMember = findTargetMemberByReportType(request.reportType(), request.targetId());

        return Report.builder()
                .reporter(reporter)
                .reportType(request.reportType())
                .targetId(request.targetId())
                .reportReason(request.reason())
                .description(request.description())
                .targetUrl(targetUrl)
                .targetMember(targetMember)
                .build();
    }

    private Member findTargetMemberByReportType(ReportType reportType, Long targetId) {
        if (ReportType.PROJECT_BOARD.equals(reportType)) {
            return findProjectBoardAuthor(targetId);
        }

        if (ReportType.STUDY_BOARD.equals(reportType)) {
            return findStudyBoardAuthor(targetId);
        }

        if (ReportType.MARKETPLACE_BOARD.equals(reportType)) {
            return findMarketplaceBoardAuthor(targetId);
        }

        if (ReportType.TUTORING_BOARD.equals(reportType)) {
            return findTutoringBoardAuthor(targetId);
        }

        if (ReportType.MEMBER.equals(reportType)) {
            return findMemberById(targetId);
        }

        throw new IllegalArgumentException("Undefined ReportType: " + reportType);
    }

    private Member findProjectBoardAuthor(Long boardId) {
        ProjectBoard board = projectBoardRepository.findById(boardId)
                .orElseThrow(() -> new ReportTargetNotFoundException("PROJECT_BOARD", boardId));
        return board.getAuthor();
    }

    private Member findStudyBoardAuthor(Long boardId) {
        StudyBoard board = studyBoardRepository.findById(boardId)
                .orElseThrow(() -> new ReportTargetNotFoundException("STUDY_BOARD", boardId));
        return board.getAuthor();
    }

    private Member findMarketplaceBoardAuthor(Long boardId) {
        MarketplaceBoard board = marketplaceBoardRepository.findById(boardId)
                .orElseThrow(() -> new ReportTargetNotFoundException("MARKETPLACE_BOARD", boardId));
        return board.getAuthor();
    }

    private Member findTutoringBoardAuthor(Long boardId) {
        TutoringBoard board = tutoringBoardRepository.findById(boardId)
                .orElseThrow(() -> new ReportTargetNotFoundException("TUTORING_BOARD", boardId));
        return board.getAuthor();
    }

    private void validateSanctionApplicable(Report report, ProcessSanctionRequest request) {
        SanctionType sanctionType = request.sanctionType();
        if (sanctionType == SanctionType.CHAT_KICK && report.getReportType() != ReportType.CHAT_MESSAGE) {
            throw new GroupChatOnlyException("채팅방 추방");
        }

        if (!report.getReportType().isMessageReport()) {
            return;
        }

        if (!request.targetMemberId().equals(report.getTargetMember().getId())) {
            throw new SanctionTargetMismatchException();
        }

        if (sanctionType == SanctionType.CONTENT_DELETION) {
            throw new UnsupportedSanctionTypeException();
        }
    }

    // 관리자 제재·기각·자동 판정이 같은 신고를 동시에 처리하지 않도록 행을 잠근 뒤 처리 여부를 검사한다
    private Report findReportById(Long reportId) {
        return reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    @Override
    @Transactional
    public SanctionStatusResponse checkAndUpdateSanctionStatus() {
        try {
            Long memberId = memberService.getMemberIdByAuthentication();
            List<Sanction> activeBans = sanctionRepository.findActiveSanctionsByMemberId(memberId).stream()
                    .filter(sanction -> sanction.getSanctionType().isBan())
                    .toList();

            activeBans.stream()
                    .filter(Sanction::isCurrentlyExpired)
                    .forEach(this::expire);

            return activeBans.stream()
                    .filter(Sanction::isSanctionActive)
                    .max(Comparator.comparing(Sanction::getEndDate))
                    .map(SanctionStatusResponse::withSanction)
                    .orElse(SanctionStatusResponse.noSanction());
        } catch (Exception e) {
            return SanctionStatusResponse.noSanction();
        }
    }

    private void expire(Sanction sanction) {
        sanction.deactivate();
        sanctionRepository.save(sanction);
    }
}