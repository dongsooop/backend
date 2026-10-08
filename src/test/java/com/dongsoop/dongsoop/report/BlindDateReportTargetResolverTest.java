package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.report.dto.CreateBlindDateReportRequest;
import com.dongsoop.dongsoop.report.dto.MessageReportDraft;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.BlindDateMessageNotFoundException;
import com.dongsoop.dongsoop.report.exception.BlindDateSessionEndedException;
import com.dongsoop.dongsoop.report.exception.NotBlindDateParticipantException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import com.dongsoop.dongsoop.report.service.BlindDateReportTargetResolver;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlindDateReportTargetResolverTest {

    private static final Long REPORTER_ID = 1L;
    private static final Long SENDER_ID = 2L;
    private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 9, 28, 21, 0);

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final BlindDateSessionStorageImpl sessions = new BlindDateSessionStorageImpl();
    private final BlindDateParticipantStorageImpl participants = new BlindDateParticipantStorageImpl();
    private final BlindDateReportTargetResolver resolver =
            new BlindDateReportTargetResolver(memberRepository, participants, sessions);

    private final Member sender = Member.builder().id(SENDER_ID).build();
    private String sessionId;

    @BeforeEach
    void setUp() {
        sessionId = sessions.create().getSessionId();
        sessions.start(sessionId);
        participants.addParticipant(sessionId, REPORTER_ID, "socket1");
        participants.addParticipant(sessionId, SENDER_ID, "socket2");
        when(memberRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
    }

    private void record(String messageId, Long senderId, String content) {
        sessions.recordMessage(sessionId, new BlindDateMessage(messageId, senderId, content, SENT_AT));
    }

    private CreateBlindDateReportRequest request(String messageId) {
        return new CreateBlindDateReportRequest(sessionId, messageId, ReportReason.INAPPROPRIATE_CONTENT, null);
    }

    @Test
    @DisplayName("신고한 메시지의 발신자를 대상으로, 세션 ID·원문·직전 맥락을 복사한다")
    void resolve_CopiesSnapshot() {
        record("m1", REPORTER_ID, "안녕");
        record("m2", SENDER_ID, "무례한 말");
        record("m3", REPORTER_ID, "뒤의 말");

        MessageReportDraft draft = resolver.resolve(REPORTER_ID, request("m2"));

        assertThat(draft.reportType()).isEqualTo(ReportType.BLINDDATE_MESSAGE);
        assertThat(draft.chatRoomId()).isEqualTo(sessionId);
        assertThat(draft.targetUrl()).isEqualTo("/blinddate/session/" + sessionId);
        assertThat(draft.targetMember()).isEqualTo(sender);
        assertThat(draft.messageId()).isEqualTo("m2");
        assertThat(draft.messageContent()).isEqualTo("무례한 말");
        assertThat(draft.messageSentAt()).isEqualTo(SENT_AT);
        assertThat(draft.messageContext().messages()).extracting(ChatMessageSnapshot::content)
                .containsExactly("안녕");
    }

    @Test
    @DisplayName("맥락은 신고 메시지 직전 10개만 오래된 순으로 담는다")
    void resolve_KeepsLastTenBefore() {
        IntStream.range(0, 15).forEach(i -> record("c" + i, REPORTER_ID, "말" + i));
        record("target", SENDER_ID, "무례한 말");

        assertThat(resolver.resolve(REPORTER_ID, request("target")).messageContext().messages())
                .extracting(ChatMessageSnapshot::content)
                .containsExactly("말5", "말6", "말7", "말8", "말9", "말10", "말11", "말12", "말13", "말14");
    }

    @Test
    @DisplayName("1,000자를 넘는 원문과 맥락은 잘라서 복사한다")
    void resolve_TruncatesLongContent() {
        record("m1", REPORTER_ID, "나".repeat(3000));
        record("m2", SENDER_ID, "가".repeat(3000));

        MessageReportDraft draft = resolver.resolve(REPORTER_ID, request("m2"));

        assertThat(draft.messageContent()).hasSize(1000);
        assertThat(draft.messageContext().messages().get(0).content()).hasSize(1000);
    }

    @Test
    @DisplayName("과팅 참가 기록이 없는 회원은 403")
    void resolve_NotParticipant_Throws() {
        record("m2", SENDER_ID, "무례한 말");

        assertThatThrownBy(() -> resolver.resolve(99L, request("m2")))
                .isInstanceOf(NotBlindDateParticipantException.class);
    }

    @Test
    @DisplayName("다른 세션 참가자가 이 세션 메시지를 신고하면 403")
    void resolve_ParticipantOfOtherSession_Throws() {
        String otherSessionId = sessions.create().getSessionId();
        participants.addParticipant(otherSessionId, 3L, "socket3");
        record("m2", SENDER_ID, "무례한 말");

        assertThatThrownBy(() -> resolver.resolve(3L, request("m2")))
                .isInstanceOf(NotBlindDateParticipantException.class);
    }

    @Test
    @DisplayName("대기 중 퇴장으로 참가 기록이 지워진 회원은 403")
    void resolve_RemovedParticipant_Throws() {
        record("m2", SENDER_ID, "무례한 말");
        participants.removeParticipant(REPORTER_ID);

        assertThatThrownBy(() -> resolver.resolve(REPORTER_ID, request("m2")))
                .isInstanceOf(NotBlindDateParticipantException.class);
    }

    @Test
    @DisplayName("진행 중 연결이 끊겨도 참가 기록이 남아 있으면 신고할 수 있다")
    void resolve_DisconnectedDuringProcessing_Allowed() {
        record("m2", SENDER_ID, "무례한 말");
        participants.removeSocket("socket1");

        assertThat(resolver.resolve(REPORTER_ID, request("m2")).messageId()).isEqualTo("m2");
    }

    @Test
    @DisplayName("세션 종료 후에는 참가 기록이 남아도 410으로 거절한다")
    void resolve_TerminatedSession_Throws() {
        record("m2", SENDER_ID, "무례한 말");
        sessions.terminate(sessionId);

        assertThatThrownBy(() -> resolver.resolve(REPORTER_ID, request("m2")))
                .isInstanceOf(BlindDateSessionEndedException.class)
                .hasMessage("종료된 과팅의 메시지는 신고할 수 없습니다.");
    }

    @Test
    @DisplayName("관리자 초기화로 세션과 참가 기록이 모두 지워져도 403이 아니라 410으로 거절한다")
    void resolve_ClearedByAdmin_ThrowsGone() {
        record("m2", SENDER_ID, "무례한 말");
        sessions.clear();
        participants.clear();

        assertThatThrownBy(() -> resolver.resolve(REPORTER_ID, request("m2")))
                .isInstanceOf(BlindDateSessionEndedException.class);
    }

    @Test
    @DisplayName("없는 메시지는 enum·ID를 노출하지 않는 문구로 404")
    void resolve_UnknownMessage_Throws() {
        assertThatThrownBy(() -> resolver.resolve(REPORTER_ID, request("missing")))
                .isInstanceOf(BlindDateMessageNotFoundException.class)
                .hasMessage("신고할 메시지를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("자기 메시지는 신고할 수 없다")
    void resolve_SelfReport_Throws() {
        record("m1", REPORTER_ID, "내 말");

        assertThatThrownBy(() -> resolver.resolve(REPORTER_ID, request("m1")))
                .isInstanceOf(SelfReportException.class);
    }
}
