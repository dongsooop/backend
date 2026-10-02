package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.CreateChatReportRequest;
import com.dongsoop.dongsoop.report.dto.CreateServerMessageReportRequest;
import com.dongsoop.dongsoop.report.dto.CreateServerMessageReportRequest.ContextMessage;
import com.dongsoop.dongsoop.report.dto.MessageReportDraft;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.DuplicateReportException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import com.dongsoop.dongsoop.report.exception.UnsupportedReportTypeException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.ChatReportTargetResolver;
import com.dongsoop.dongsoop.report.service.ReportServiceImpl;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
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

    private final Member reporter = Member.builder().id(1L).build();
    private final Member sender = Member.builder().id(2L).build();
    private final CreateChatReportRequest chatRequest =
            new CreateChatReportRequest("room1", "target", ReportReason.HATE_SPEECH, "욕함");
    private final ChatMessageSnapshots context =
            new ChatMessageSnapshots(List.of(ChatMessageSnapshot.of(1L, "앞말", SENT_AT.minusMinutes(1))));

    @BeforeEach
    void setUp() {
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);
        when(memberRepository.getReferenceById(1L)).thenReturn(reporter);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(sender));
        when(chatReportTargetResolver.resolve(1L, chatRequest)).thenReturn(new MessageReportDraft(
                ReportType.CHAT_MESSAGE, "room1", "/chat/room/room1", sender, "target", "욕", SENT_AT, context));
    }

    private static CreateServerMessageReportRequest blindDateRequest(ReportType reportType, Long targetMemberId,
                                                                     String content, List<ContextMessage> context) {
        return new CreateServerMessageReportRequest(reportType, 1L, targetMemberId, "s1", "m2", content, SENT_AT,
                context, ReportReason.INAPPROPRIATE_CONTENT, "무례함");
    }

    private static CreateServerMessageReportRequest blindDateRequest() {
        return blindDateRequest(ReportType.BLINDDATE_MESSAGE, 2L, "무례한 말",
                List.of(new ContextMessage(1L, "앞말", SENT_AT.minusMinutes(1))));
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
    @DisplayName("과팅 서버가 보낸 신고를 세션·대상·메시지 스냅샷과 함께 같은 흐름으로 저장한다")
    void createServerMessageReport_SavesDraftWithReporterAndReason() {
        reportService.createServerMessageReport(blindDateRequest());

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
    @DisplayName("1,000자를 넘는 과팅 메시지와 맥락은 잘라서 저장한다")
    void createServerMessageReport_TruncatesLongContent() {
        reportService.createServerMessageReport(blindDateRequest(ReportType.BLINDDATE_MESSAGE, 2L,
                "가".repeat(3000), List.of(new ContextMessage(1L, "나".repeat(3000), SENT_AT))));

        Report report = savedReport();
        assertThat(report.getMessageContent()).hasSize(1000);
        assertThat(report.getMessageContext().messages().get(0).content()).hasSize(1000);
    }

    @Test
    @DisplayName("맥락이 10개를 넘으면 마지막 10개만 저장한다")
    void createServerMessageReport_KeepsLastTenContextMessages() {
        List<ContextMessage> longContext = IntStream.range(0, 15)
                .mapToObj(i -> new ContextMessage(1L, "말" + i, SENT_AT.minusMinutes(15L - i)))
                .toList();

        reportService.createServerMessageReport(blindDateRequest(ReportType.BLINDDATE_MESSAGE, 2L, "무례한 말",
                longContext));

        assertThat(savedReport().getMessageContext().messages()).extracting(ChatMessageSnapshot::content)
                .containsExactly("말5", "말6", "말7", "말8", "말9", "말10", "말11", "말12", "말13", "말14");
    }

    @Test
    @DisplayName("맥락이 없으면 빈 맥락으로 저장한다")
    void createServerMessageReport_WithoutContext_SavesEmptyContext() {
        reportService.createServerMessageReport(blindDateRequest(ReportType.BLINDDATE_MESSAGE, 2L, "무례한 말",
                null));

        assertThat(savedReport().getMessageContext().messages()).isEmpty();
    }

    @Test
    @DisplayName("서버 간 신고 API는 과팅 메시지 외의 신고 유형을 받지 않는다")
    void createServerMessageReport_UnsupportedType_Throws() {
        assertThatThrownBy(() -> reportService.createServerMessageReport(blindDateRequest(
                ReportType.CHAT_MESSAGE, 2L, "욕", List.of())))
                .isInstanceOf(UnsupportedReportTypeException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("과팅에서 자기 메시지는 신고할 수 없다")
    void createServerMessageReport_SelfReport_Throws() {
        assertThatThrownBy(() -> reportService.createServerMessageReport(blindDateRequest(
                ReportType.BLINDDATE_MESSAGE, 1L, "내 말", List.of())))
                .isInstanceOf(SelfReportException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("신고자가 없는 회원이면 저장하지 않는다")
    void createServerMessageReport_UnknownReporter_Throws() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createServerMessageReport(blindDateRequest()))
                .isInstanceOf(MemberNotFoundException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("신고 대상이 없는 회원이면 저장하지 않는다")
    void createServerMessageReport_UnknownTarget_Throws() {
        when(memberRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createServerMessageReport(blindDateRequest()))
                .isInstanceOf(MemberNotFoundException.class);
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
    void createServerMessageReport_SameTargetInSession_Throws() {
        when(reportRepository.existsByReporterIdAndReportTypeAndChatRoomIdAndTargetMemberId(
                1L, ReportType.BLINDDATE_MESSAGE, "s1", 2L)).thenReturn(true);

        assertThatThrownBy(() -> reportService.createServerMessageReport(blindDateRequest()))
                .isInstanceOf(DuplicateReportException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("과팅 동시 신고로 유니크 인덱스에 걸리면 중복 신고로 응답한다")
    void createServerMessageReport_UniqueViolation_ThrowsDuplicate() {
        when(reportRepository.saveAndFlush(any())).thenThrow(violation("uk_report_blinddate_reporter_target"));

        assertThatThrownBy(() -> reportService.createServerMessageReport(blindDateRequest()))
                .isInstanceOf(DuplicateReportException.class);
    }

    @Test
    @DisplayName("같은 메시지 재신고로 채팅 유니크 인덱스에 걸려도 과팅 신고는 중복 신고로 응답한다")
    void createServerMessageReport_MessageUniqueViolation_ThrowsDuplicate() {
        when(reportRepository.saveAndFlush(any())).thenThrow(violation("uk_report_reporter_message"));

        assertThatThrownBy(() -> reportService.createServerMessageReport(blindDateRequest()))
                .isInstanceOf(DuplicateReportException.class);
    }

    @Test
    @DisplayName("과팅 신고에서 다른 제약 위반은 중복 신고로 바꾸지 않고 그대로 전파한다")
    void createServerMessageReport_OtherConstraintViolation_Propagates() {
        DataIntegrityViolationException thrown = violation("fk_report_target_member");
        when(reportRepository.saveAndFlush(any())).thenThrow(thrown);

        assertThatThrownBy(() -> reportService.createServerMessageReport(blindDateRequest()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isSameAs(thrown);
    }
}
