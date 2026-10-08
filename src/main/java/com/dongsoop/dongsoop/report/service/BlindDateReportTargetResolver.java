package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo;
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
import com.dongsoop.dongsoop.report.exception.BlindDateMessageNotFoundException;
import com.dongsoop.dongsoop.report.exception.BlindDateSessionEndedException;
import com.dongsoop.dongsoop.report.exception.NotBlindDateParticipantException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import java.util.List;
import java.util.Optional;
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
        String sessionId = request.sessionId();
        // 세션 종료·관리자 초기화·재배포 모두 세션 메모리가 비어 메시지 기록이 남지 않으므로 참가자 검사보다 먼저 410으로 모은다
        if (sessionStorage.getState(sessionId) == null) {
            throw new BlindDateSessionEndedException();
        }
        validateParticipant(reporterId, sessionId);

        BlindDateMessage target = sessionStorage.findMessage(sessionId, request.messageId())
                .orElseThrow(BlindDateMessageNotFoundException::new);

        if (target.senderId().equals(reporterId)) {
            throw new SelfReportException();
        }

        Member targetMember = memberRepository.findById(target.senderId())
                .orElseThrow(MemberNotFoundException::new);

        return new MessageReportDraft(ReportType.BLINDDATE_MESSAGE, sessionId, "/blinddate/session/" + sessionId,
                targetMember, target.messageId(), ChatMessageSnapshot.truncate(target.content()), target.sentAt(),
                buildContext(sessionId, target.messageId()));
    }

    /**
     * 진행 중인 세션 메모리에서 신고된 메시지 다음 메시지를 최대 10개 반환한다. 세션이 없으면 비어 있다.
     */
    public Optional<ChatMessageSnapshots> findLiveContextAfter(String sessionId, String messageId) {
        if (sessionStorage.getState(sessionId) == null) {
            return Optional.empty();
        }

        return Optional.of(toSnapshots(sessionStorage.findMessagesAfter(sessionId, messageId, CONTEXT_SIZE)));
    }

    /**
     * 세션 종료 때 넘겨받은 메시지 기록에서 신고된 메시지 다음 메시지를 최대 10개 고른다.
     */
    public static ChatMessageSnapshots contextAfter(List<BlindDateMessage> messages, String messageId) {
        return toSnapshots(SessionInfo.messagesAfter(messages, messageId, CONTEXT_SIZE));
    }

    // 참가자 기록은 대기 중 퇴장과 전체 초기화 때만 지워지고, 진행 중 연결이 끊기거나 세션이 종료돼도 남는다
    private void validateParticipant(Long reporterId, String sessionId) {
        ParticipantInfo participant = participantStorage.getByMemberId(reporterId);
        if (participant == null || !participant.getSessionId().equals(sessionId)) {
            throw new NotBlindDateParticipantException();
        }
    }

    private ChatMessageSnapshots buildContext(String sessionId, String messageId) {
        return toSnapshots(sessionStorage.findMessagesBefore(sessionId, messageId, CONTEXT_SIZE));
    }

    private static ChatMessageSnapshots toSnapshots(List<BlindDateMessage> messages) {
        return new ChatMessageSnapshots(messages.stream()
                .map(message -> ChatMessageSnapshot.of(message.senderId(), message.content(), message.sentAt()))
                .toList());
    }
}
