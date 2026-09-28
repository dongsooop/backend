package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.report.dto.CreateBlindDateReportRequest;
import com.dongsoop.dongsoop.report.dto.MessageReportDraft;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.NotBlindDateParticipantException;
import com.dongsoop.dongsoop.report.exception.ReportTargetNotFoundException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import com.dongsoop.dongsoop.report.service.BlindDateReportTargetResolver;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlindDateReportTargetResolverTest {

    private static final String SESSION_ID = "s1";
    private static final Long REPORTER_ID = 1L;
    private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 9, 28, 21, 0);

    @InjectMocks
    private BlindDateReportTargetResolver resolver;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BlindDateParticipantStorage participantStorage;
    @Mock
    private BlindDateSessionStorage sessionStorage;

    private final Member sender = Member.builder().id(2L).build();

    @BeforeEach
    void setUp() {
        when(participantStorage.getByMemberId(1L))
                .thenReturn(ParticipantInfo.create(SESSION_ID, 1L, "socket1", "익명1"));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(sender));
        when(sessionStorage.findMessage(SESSION_ID, "m2"))
                .thenReturn(Optional.of(new BlindDateMessage("m2", 2L, "무례한 말", SENT_AT)));
        when(sessionStorage.findMessagesBefore(SESSION_ID, "m2", 10))
                .thenReturn(List.of(new BlindDateMessage("m1", 1L, "안녕", SENT_AT.minusMinutes(1))));
    }

    private static CreateBlindDateReportRequest request(String messageId) {
        return new CreateBlindDateReportRequest(messageId, ReportReason.INAPPROPRIATE_CONTENT, null);
    }

    @Test
    @DisplayName("과팅 메시지를 세션 ID·내용·맥락과 함께 복사한다")
    void resolve_CopiesSnapshot() {
        MessageReportDraft draft = resolver.resolve(REPORTER_ID, request("m2"));

        assertThat(draft.reportType()).isEqualTo(ReportType.BLINDDATE_MESSAGE);
        assertThat(draft.chatRoomId()).isEqualTo(SESSION_ID);
        assertThat(draft.targetMember()).isEqualTo(sender);
        assertThat(draft.messageContent()).isEqualTo("무례한 말");
        assertThat(draft.targetUrl()).isEqualTo("/blinddate/session/" + SESSION_ID);
        assertThat(draft.messageContext().messages()).extracting(snapshot -> snapshot.content())
                .containsExactly("안녕");
    }

    @Test
    @DisplayName("1,000자를 넘는 과팅 메시지는 잘라서 복사한다")
    void resolve_TruncatesLongMessage() {
        when(sessionStorage.findMessage(SESSION_ID, "long"))
                .thenReturn(Optional.of(new BlindDateMessage("long", 2L, "가".repeat(3000), SENT_AT)));

        assertThat(resolver.resolve(REPORTER_ID, request("long")).messageContent()).hasSize(1000);
    }

    @Test
    @DisplayName("과팅 참여자가 아니면 신고할 수 없다")
    void resolve_NotParticipant_Throws() {
        when(participantStorage.getByMemberId(1L)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(REPORTER_ID, request("m2")))
                .isInstanceOf(NotBlindDateParticipantException.class);
    }

    @Test
    @DisplayName("세션에 없는 메시지는 찾을 수 없다")
    void resolve_UnknownMessage_Throws() {
        when(sessionStorage.findMessage(SESSION_ID, "none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(REPORTER_ID, request("none")))
                .isInstanceOf(ReportTargetNotFoundException.class);
    }

    @Test
    @DisplayName("자기 메시지는 신고할 수 없다")
    void resolve_OwnMessage_Throws() {
        when(sessionStorage.findMessage(SESSION_ID, "mine"))
                .thenReturn(Optional.of(new BlindDateMessage("mine", 1L, "내 말", SENT_AT)));

        assertThatThrownBy(() -> resolver.resolve(REPORTER_ID, request("mine")))
                .isInstanceOf(SelfReportException.class);
    }
}
