package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.report.dto.CreateBlindDateReportRequest;
import com.dongsoop.dongsoop.report.dto.MessageReportDraft;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.NotBlindDateParticipantException;
import com.dongsoop.dongsoop.report.exception.ReportTargetNotFoundException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlindDateReportTargetResolver {

    private static final int CONTEXT_SIZE = 10;

    private final MemberRepository memberRepository;
    private final BlindDateParticipantStorage participantStorage;
    private final BlindDateSessionStorage sessionStorage;

    public MessageReportDraft resolve(Long reporterId, CreateBlindDateReportRequest request) {
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

        Member targetMember = memberRepository.findById(target.senderId())
                .orElseThrow(MemberNotFoundException::new);

        return new MessageReportDraft(ReportType.BLINDDATE_MESSAGE, sessionId, "/blinddate/session/" + sessionId,
                targetMember, target.messageId(), ChatMessageSnapshot.truncate(target.content()), target.sentAt(),
                new ChatMessageSnapshots(
                        sessionStorage.findMessagesBefore(sessionId, target.messageId(), CONTEXT_SIZE).stream()
                                .map(message -> ChatMessageSnapshot.of(
                                        message.senderId(), message.content(), message.sentAt()))
                                .toList()));
    }
}
