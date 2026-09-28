package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.CreateBlindDateReportRequest;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.DuplicateReportException;
import com.dongsoop.dongsoop.report.exception.NotBlindDateParticipantException;
import com.dongsoop.dongsoop.report.exception.ReportTargetNotFoundException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlindDateReportService {

    private static final int CONTEXT_SIZE = 10;
    private static final Set<String> DUPLICATE_REPORT_CONSTRAINT_NAMES =
            Set.of("uk_report_blinddate_reporter_target", "uk_report_reporter_message");

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final BlindDateParticipantStorage participantStorage;
    private final BlindDateSessionStorage sessionStorage;
    private final ReportRepository reportRepository;

    @Transactional
    public void createReport(CreateBlindDateReportRequest request) {
        Long reporterId = memberService.getMemberIdByAuthentication();
        ParticipantInfo participant = participantStorage.getByMemberId(reporterId);
        if (participant == null) {
            throw new NotBlindDateParticipantException();
        }

        String sessionId = participant.getSessionId();
        BlindDateMessage target = sessionStorage.findMessage(sessionId, request.messageId())
                .orElseThrow(() -> new ReportTargetNotFoundException(
                        ReportType.BLINDDATE_MESSAGE.name(), request.messageId()));

        if (target.senderId().equals(reporterId)) {
            throw new SelfReportException();
        }

        if (reportRepository.existsByReporterIdAndReportTypeAndChatRoomIdAndTargetMemberId(
                reporterId, ReportType.BLINDDATE_MESSAGE, sessionId, target.senderId())) {
            throw new DuplicateReportException();
        }

        Member targetMember = memberRepository.findById(target.senderId())
                .orElseThrow(MemberNotFoundException::new);

        save(Report.builder()
                .reporter(memberRepository.getReferenceById(reporterId))
                .reportType(ReportType.BLINDDATE_MESSAGE)
                .targetId(targetMember.getId())
                .targetMember(targetMember)
                .reportReason(request.reason())
                .description(request.description())
                .targetUrl("/blinddate/session/" + sessionId)
                .chatRoomId(sessionId)
                .messageId(target.messageId())
                .messageContent(ChatMessageSnapshot.truncate(target.content()))
                .messageSentAt(target.sentAt())
                .messageContext(new ChatMessageSnapshots(
                        sessionStorage.findMessagesBefore(sessionId, target.messageId(), CONTEXT_SIZE).stream()
                                .map(message -> ChatMessageSnapshot.of(
                                        message.senderId(), message.content(), message.sentAt()))
                                .toList()))
                .build());
    }

    private void save(Report report) {
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
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                return constraintName != null
                        && DUPLICATE_REPORT_CONSTRAINT_NAMES.contains(constraintName.toLowerCase(Locale.ROOT));
            }
            cause = cause.getCause();
        }
        return false;
    }
}
