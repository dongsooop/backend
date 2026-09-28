package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.CreateBlindDateReportRequest;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.DuplicateReportException;
import com.dongsoop.dongsoop.report.exception.NotBlindDateParticipantException;
import com.dongsoop.dongsoop.report.exception.ReportTargetNotFoundException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.BlindDateReportService;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
class BlindDateReportServiceTest {

    private static final String SESSION_ID = "s1";
    private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 9, 28, 21, 0);

    @InjectMocks
    private BlindDateReportService blindDateReportService;

    @Mock
    private MemberService memberService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BlindDateParticipantStorage participantStorage;
    @Mock
    private BlindDateSessionStorage sessionStorage;
    @Mock
    private ReportRepository reportRepository;

    private final Member reporter = Member.builder().id(1L).build();
    private final Member sender = Member.builder().id(2L).build();

    @BeforeEach
    void setUp() {
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);
        when(participantStorage.getByMemberId(1L))
                .thenReturn(ParticipantInfo.create(SESSION_ID, 1L, "socket1", "익명1"));
        when(memberRepository.getReferenceById(1L)).thenReturn(reporter);
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
    @DisplayName("과팅 메시지를 세션 ID·내용·맥락과 함께 저장한다")
    void createReport_SavesSnapshot() {
        blindDateReportService.createReport(request("m2"));

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        Report report = captor.getValue();
        assertThat(report.getReportType()).isEqualTo(ReportType.BLINDDATE_MESSAGE);
        assertThat(report.getChatRoomId()).isEqualTo(SESSION_ID);
        assertThat(report.getTargetMember()).isEqualTo(sender);
        assertThat(report.getMessageContent()).isEqualTo("무례한 말");
        assertThat(report.getTargetUrl()).isEqualTo("/blinddate/session/" + SESSION_ID);
        assertThat(report.getMessageContext().messages()).extracting(snapshot -> snapshot.content())
                .containsExactly("안녕");
    }

    @Test
    @DisplayName("1,000자를 넘는 과팅 메시지는 잘라서 저장한다")
    void createReport_TruncatesLongMessage() {
        when(sessionStorage.findMessage(SESSION_ID, "long"))
                .thenReturn(Optional.of(new BlindDateMessage("long", 2L, "가".repeat(3000), SENT_AT)));

        blindDateReportService.createReport(request("long"));

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getMessageContent()).hasSize(1000);
    }

    @Test
    @DisplayName("과팅 참여자가 아니면 신고할 수 없다")
    void createReport_NotParticipant_Throws() {
        when(participantStorage.getByMemberId(1L)).thenReturn(null);

        assertThatThrownBy(() -> blindDateReportService.createReport(request("m2")))
                .isInstanceOf(NotBlindDateParticipantException.class);
    }

    @Test
    @DisplayName("세션에 없는 메시지는 찾을 수 없다")
    void createReport_UnknownMessage_Throws() {
        when(sessionStorage.findMessage(SESSION_ID, "none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blindDateReportService.createReport(request("none")))
                .isInstanceOf(ReportTargetNotFoundException.class);
    }

    @Test
    @DisplayName("자기 메시지는 신고할 수 없다")
    void createReport_OwnMessage_Throws() {
        when(sessionStorage.findMessage(SESSION_ID, "mine"))
                .thenReturn(Optional.of(new BlindDateMessage("mine", 1L, "내 말", SENT_AT)));

        assertThatThrownBy(() -> blindDateReportService.createReport(request("mine")))
                .isInstanceOf(SelfReportException.class);
    }

    @Test
    @DisplayName("같은 세션에서 같은 상대는 한 번만 신고할 수 있다")
    void createReport_SameTargetInSession_Throws() {
        when(reportRepository.existsByReporterIdAndReportTypeAndChatRoomIdAndTargetMemberId(
                1L, ReportType.BLINDDATE_MESSAGE, SESSION_ID, 2L)).thenReturn(true);

        assertThatThrownBy(() -> blindDateReportService.createReport(request("m2")))
                .isInstanceOf(DuplicateReportException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("동시 신고로 유니크 인덱스에 걸리면 중복 신고로 응답한다")
    void createReport_UniqueViolation_ThrowsDuplicate() {
        ConstraintViolationException cause = new ConstraintViolationException(
                "duplicate key", new SQLException("duplicate key"), "uk_report_blinddate_reporter_target");
        when(reportRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk", cause));

        assertThatThrownBy(() -> blindDateReportService.createReport(request("m2")))
                .isInstanceOf(DuplicateReportException.class);
    }

    @Test
    @DisplayName("다른 제약 위반은 중복 신고로 바꾸지 않고 그대로 전파한다")
    void createReport_OtherConstraintViolation_Propagates() {
        ConstraintViolationException cause = new ConstraintViolationException(
                "fk violation", new SQLException("fk"), "fk_report_target_member");
        DataIntegrityViolationException thrown = new DataIntegrityViolationException("fk", cause);
        when(reportRepository.saveAndFlush(any())).thenThrow(thrown);

        assertThatThrownBy(() -> blindDateReportService.createReport(request("m2")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isSameAs(thrown);
    }
}
