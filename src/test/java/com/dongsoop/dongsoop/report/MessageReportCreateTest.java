package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.CreateBlindDateReportRequest;
import com.dongsoop.dongsoop.report.dto.CreateChatReportRequest;
import com.dongsoop.dongsoop.report.dto.MessageReportDraft;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.DuplicateReportException;
import com.dongsoop.dongsoop.report.exception.NotBlindDateParticipantException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.BlindDateReportTargetResolver;
import com.dongsoop.dongsoop.report.service.ChatReportTargetResolver;
import com.dongsoop.dongsoop.report.service.ReportServiceImpl;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageReportCreateTest {

    private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 9, 28, 21, 0);

    @InjectMocks
    private ReportServiceImpl reportService;

    @Mock
    private MemberService memberService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ChatReportTargetResolver chatReportTargetResolver;
    @Mock
    private BlindDateReportTargetResolver blindDateReportTargetResolver;

    private final Member reporter = Member.builder().id(1L).build();
    private final Member sender = Member.builder().id(2L).build();
    private final CreateChatReportRequest chatRequest =
            new CreateChatReportRequest("room1", "target", ReportReason.HATE_SPEECH, "욕함");
    private final CreateBlindDateReportRequest blindDateRequest =
            new CreateBlindDateReportRequest("s1", "m2", ReportReason.INAPPROPRIATE_CONTENT, "무례함");
    private final ChatMessageSnapshots context =
            new ChatMessageSnapshots(List.of(ChatMessageSnapshot.of(1L, "앞말", SENT_AT.minusMinutes(1))));

    @BeforeEach
    void setUp() {
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);
        when(memberRepository.getReferenceById(1L)).thenReturn(reporter);
        when(chatReportTargetResolver.resolve(1L, chatRequest)).thenReturn(new MessageReportDraft(
                ReportType.CHAT_MESSAGE, "room1", "/chat/room/room1", sender, "target", "욕", SENT_AT, context));
        when(blindDateReportTargetResolver.resolve(1L, blindDateRequest)).thenReturn(new MessageReportDraft(
                ReportType.BLINDDATE_MESSAGE, "s1", "/blinddate/session/s1", sender, "m2", "무례한 말", SENT_AT,
                context));
    }

    private Report savedReport() {
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static DataIntegrityViolationException violation(String constraintName) {
        return new DataIntegrityViolationException(constraintName, new ConstraintViolationException(
                "violation", new SQLException("violation"), constraintName));
    }

    @Test
    @DisplayName("채팅 신고는 리졸버가 확인한 대상과 신고 내용을 합쳐 저장한다")
    void createChatReport_SavesDraftWithReporterAndReason() {
        reportService.createChatReport(chatRequest);

        Report report = savedReport();
        assertThat(report.getReporter()).isEqualTo(reporter);
        assertThat(report.getReportType()).isEqualTo(ReportType.CHAT_MESSAGE);
        assertThat(report.getTargetMember()).isEqualTo(sender);
        assertThat(report.getTargetId()).isEqualTo(2L);
        assertThat(report.getReportReason()).isEqualTo(ReportReason.HATE_SPEECH);
        assertThat(report.getDescription()).isEqualTo("욕함");
        assertThat(report.getTargetUrl()).isEqualTo("/chat/room/room1");
        assertThat(report.getChatRoomId()).isEqualTo("room1");
        assertThat(report.getMessageId()).isEqualTo("target");
        assertThat(report.getMessageContent()).isEqualTo("욕");
        assertThat(report.getMessageSentAt()).isEqualTo(SENT_AT);
        assertThat(report.getMessageContext()).isEqualTo(context);
    }

    @Test
    @DisplayName("과팅 신고는 리졸버가 세션 메모리에서 확인한 대상과 신고 내용을 합쳐 저장한다")
    void createBlindDateReport_SavesDraftWithReporterAndReason() {
        reportService.createBlindDateReport(blindDateRequest);

        Report report = savedReport();
        assertThat(report.getReporter()).isEqualTo(reporter);
        assertThat(report.getReportType()).isEqualTo(ReportType.BLINDDATE_MESSAGE);
        assertThat(report.getTargetMember()).isEqualTo(sender);
        assertThat(report.getTargetId()).isEqualTo(2L);
        assertThat(report.getChatRoomId()).isEqualTo("s1");
        assertThat(report.getTargetUrl()).isEqualTo("/blinddate/session/s1");
        assertThat(report.getMessageId()).isEqualTo("m2");
        assertThat(report.getMessageContent()).isEqualTo("무례한 말");
        assertThat(report.getMessageSentAt()).isEqualTo(SENT_AT);
        assertThat(report.getMessageContext()).isEqualTo(context);
        assertThat(report.getReportReason()).isEqualTo(ReportReason.INAPPROPRIATE_CONTENT);
        assertThat(report.getDescription()).isEqualTo("무례함");
    }

    @Test
    @DisplayName("과팅 대상 확인에 실패하면 저장하지 않는다")
    void createBlindDateReport_ResolverRejects_DoesNotSave() {
        when(blindDateReportTargetResolver.resolve(1L, blindDateRequest))
                .thenThrow(new NotBlindDateParticipantException());

        assertThatThrownBy(() -> reportService.createBlindDateReport(blindDateRequest))
                .isInstanceOf(NotBlindDateParticipantException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("대상 확인에 실패하면 저장하지 않는다")
    void createChatReport_ResolverRejects_DoesNotSave() {
        when(chatReportTargetResolver.resolve(1L, chatRequest)).thenThrow(new SelfReportException());

        assertThatThrownBy(() -> reportService.createChatReport(chatRequest))
                .isInstanceOf(SelfReportException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("같은 메시지를 다시 신고하면 거절한다")
    void createChatReport_Duplicate_Throws() {
        when(reportRepository.existsByReporterIdAndMessageId(1L, "target")).thenReturn(true);

        assertThatThrownBy(() -> reportService.createChatReport(chatRequest))
                .isInstanceOf(DuplicateReportException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("동시에 두 번 신고해 유니크 인덱스에 걸리면 중복 신고로 응답한다")
    void createChatReport_UniqueViolation_ThrowsDuplicate() {
        when(reportRepository.saveAndFlush(any())).thenThrow(violation("uk_report_reporter_message"));

        assertThatThrownBy(() -> reportService.createChatReport(chatRequest))
                .isInstanceOf(DuplicateReportException.class);
    }

    @Test
    @DisplayName("채팅 신고에서 다른 제약 위반은 중복 신고로 바꾸지 않고 그대로 전파한다")
    void createChatReport_OtherConstraintViolation_Propagates() {
        DataIntegrityViolationException thrown = violation("fk_report_target_member");
        when(reportRepository.saveAndFlush(any())).thenThrow(thrown);

        assertThatThrownBy(() -> reportService.createChatReport(chatRequest))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isSameAs(thrown);
    }

    @Test
    @DisplayName("같은 세션에서 같은 상대는 한 번만 신고할 수 있다")
    void createBlindDateReport_SameTargetInSession_Throws() {
        when(reportRepository.existsByReporterIdAndReportTypeAndChatRoomIdAndTargetMemberId(
                1L, ReportType.BLINDDATE_MESSAGE, "s1", 2L)).thenReturn(true);

        assertThatThrownBy(() -> reportService.createBlindDateReport(blindDateRequest))
                .isInstanceOf(DuplicateReportException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("과팅 동시 신고로 유니크 인덱스에 걸리면 중복 신고로 응답한다")
    void createBlindDateReport_UniqueViolation_ThrowsDuplicate() {
        when(reportRepository.saveAndFlush(any())).thenThrow(violation("uk_report_blinddate_reporter_target"));

        assertThatThrownBy(() -> reportService.createBlindDateReport(blindDateRequest))
                .isInstanceOf(DuplicateReportException.class);
    }

    @Test
    @DisplayName("같은 메시지 재신고로 채팅 유니크 인덱스에 걸려도 과팅 신고는 중복 신고로 응답한다")
    void createBlindDateReport_MessageUniqueViolation_ThrowsDuplicate() {
        when(reportRepository.saveAndFlush(any())).thenThrow(violation("uk_report_reporter_message"));

        assertThatThrownBy(() -> reportService.createBlindDateReport(blindDateRequest))
                .isInstanceOf(DuplicateReportException.class);
    }

    @Test
    @DisplayName("과팅 신고에서 다른 제약 위반은 중복 신고로 바꾸지 않고 그대로 전파한다")
    void createBlindDateReport_OtherConstraintViolation_Propagates() {
        DataIntegrityViolationException thrown = violation("fk_report_target_member");
        when(reportRepository.saveAndFlush(any())).thenThrow(thrown);

        assertThatThrownBy(() -> reportService.createBlindDateReport(blindDateRequest))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isSameAs(thrown);
    }
}
