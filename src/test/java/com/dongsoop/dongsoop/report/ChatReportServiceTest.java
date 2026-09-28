package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatMessageEntity;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.repository.ChatMessageJpaRepository;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.CreateChatReportRequest;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.DuplicateReportException;
import com.dongsoop.dongsoop.report.exception.NotReportableMessageException;
import com.dongsoop.dongsoop.report.exception.ReportTargetNotFoundException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.ChatReportService;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
class ChatReportServiceTest {

    private static final String ROOM_ID = "room1";
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 9, 28, 21, 0);

    @InjectMocks
    private ChatReportService chatReportService;

    @Mock
    private MemberService memberService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private RedisChatRepository redisChatRepository;
    @Mock
    private ChatMessageJpaRepository chatMessageJpaRepository;
    @Mock
    private ReportRepository reportRepository;

    private final Member reporter = Member.builder().id(1L).build();
    private final Member sender = Member.builder().id(2L).build();

    @BeforeEach
    void setUp() {
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);
        when(chatRoomService.getChatRoomById(ROOM_ID)).thenReturn(ChatRoom.builder()
                .roomId(ROOM_ID)
                .participants(new HashSet<>(Set.of(1L, 2L)))
                .build());
        when(memberRepository.getReferenceById(1L)).thenReturn(reporter);
        when(memberRepository.findById(2L)).thenReturn(Optional.of(sender));
    }

    private static ChatMessage message(String id, Long senderId, int minute, MessageType type) {
        return ChatMessage.builder().messageId(id).roomId(ROOM_ID).senderId(senderId)
                .content("내용" + id).timestamp(BASE.plusMinutes(minute)).type(type).build();
    }

    private Report savedReport() {
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static CreateChatReportRequest request(String messageId) {
        return new CreateChatReportRequest(ROOM_ID, messageId, ReportReason.HATE_SPEECH, "욕함");
    }

    @Test
    @DisplayName("메시지 내용과 직전 사용자 메시지 최대 10개를 복사해 저장한다")
    void createReport_SavesSnapshotAndContext() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(message("enter", 2L, 0, MessageType.ENTER));
        for (int i = 1; i <= 12; i++) {
            messages.add(message("m" + i, i % 2 == 0 ? 2L : 1L, i, MessageType.CHAT));
        }
        messages.add(message("target", 2L, 13, MessageType.CHAT));
        messages.add(message("after", 1L, 14, MessageType.CHAT));
        when(redisChatRepository.findMessagesByRoomId(ROOM_ID)).thenReturn(messages);

        chatReportService.createReport(request("target"));

        Report report = savedReport();
        assertThat(report.getReportType()).isEqualTo(ReportType.CHAT_MESSAGE);
        assertThat(report.getTargetMember()).isEqualTo(sender);
        assertThat(report.getTargetId()).isEqualTo(2L);
        assertThat(report.getReporter()).isEqualTo(reporter);
        assertThat(report.getChatRoomId()).isEqualTo(ROOM_ID);
        assertThat(report.getMessageId()).isEqualTo("target");
        assertThat(report.getMessageContent()).isEqualTo("내용target");
        assertThat(report.getMessageSentAt()).isEqualTo(BASE.plusMinutes(13));
        assertThat(report.getTargetUrl()).isEqualTo("/chat/room/" + ROOM_ID);
        assertThat(report.getMessageContext().messages())
                .extracting(snapshot -> snapshot.content())
                .containsExactly("내용m3", "내용m4", "내용m5", "내용m6", "내용m7",
                        "내용m8", "내용m9", "내용m10", "내용m11", "내용m12");
    }

    @Test
    @DisplayName("Redis에 없는 메시지는 DB 백업에서 찾는다")
    void createReport_FallsBackToDatabase() {
        when(redisChatRepository.findMessagesByRoomId(ROOM_ID)).thenReturn(List.of());
        when(chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(ROOM_ID)).thenReturn(List.of(
                ChatMessageEntity.builder().messageId("old").roomId(ROOM_ID).senderId(2L)
                        .content("옛날 욕").timestamp(BASE).type(MessageType.CHAT).build()));

        chatReportService.createReport(request("old"));

        assertThat(savedReport().getMessageContent()).isEqualTo("옛날 욕");
    }

    @Test
    @DisplayName("타입이 비어 있는 과거 메시지는 사용자 메시지로 보고 신고를 받는다")
    void createReport_NullTypeIsUserMessage() {
        when(redisChatRepository.findMessagesByRoomId(ROOM_ID))
                .thenReturn(List.of(message("legacy", 2L, 0, null)));

        chatReportService.createReport(request("legacy"));

        assertThat(savedReport().getMessageId()).isEqualTo("legacy");
    }

    @Test
    @DisplayName("방 참여자가 아니면 신고할 수 없다")
    void createReport_NotParticipant_Throws() {
        when(memberService.getMemberIdByAuthentication()).thenReturn(3L);

        assertThatThrownBy(() -> chatReportService.createReport(request("target")))
                .isInstanceOf(UnauthorizedChatAccessException.class);
    }

    @Test
    @DisplayName("방에 없는 메시지는 찾을 수 없다")
    void createReport_UnknownMessage_Throws() {
        when(redisChatRepository.findMessagesByRoomId(ROOM_ID)).thenReturn(List.of());
        when(chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(ROOM_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> chatReportService.createReport(request("nope")))
                .isInstanceOf(ReportTargetNotFoundException.class);
    }

    @Test
    @DisplayName("입장·퇴장 시스템 메시지는 신고할 수 없다")
    void createReport_SystemMessage_Throws() {
        when(redisChatRepository.findMessagesByRoomId(ROOM_ID))
                .thenReturn(List.of(message("leave", 2L, 0, MessageType.LEAVE)));

        assertThatThrownBy(() -> chatReportService.createReport(request("leave")))
                .isInstanceOf(NotReportableMessageException.class);
    }

    @Test
    @DisplayName("자기 메시지는 신고할 수 없다")
    void createReport_OwnMessage_Throws() {
        when(redisChatRepository.findMessagesByRoomId(ROOM_ID))
                .thenReturn(List.of(message("mine", 1L, 0, MessageType.CHAT)));

        assertThatThrownBy(() -> chatReportService.createReport(request("mine")))
                .isInstanceOf(SelfReportException.class);
    }

    @Test
    @DisplayName("같은 메시지를 다시 신고하면 거절한다")
    void createReport_Duplicate_Throws() {
        when(redisChatRepository.findMessagesByRoomId(ROOM_ID))
                .thenReturn(List.of(message("target", 2L, 0, MessageType.CHAT)));
        when(reportRepository.existsByReporterIdAndMessageId(1L, "target")).thenReturn(true);

        assertThatThrownBy(() -> chatReportService.createReport(request("target")))
                .isInstanceOf(DuplicateReportException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("동시에 두 번 신고해 유니크 인덱스에 걸리면 중복 신고로 응답한다")
    void createReport_UniqueViolation_ThrowsDuplicate() {
        when(redisChatRepository.findMessagesByRoomId(ROOM_ID))
                .thenReturn(List.of(message("target", 2L, 0, MessageType.CHAT)));
        ConstraintViolationException cause = new ConstraintViolationException(
                "duplicate key", new SQLException("duplicate key"), "uk_report_reporter_message");
        when(reportRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk", cause));

        assertThatThrownBy(() -> chatReportService.createReport(request("target")))
                .isInstanceOf(DuplicateReportException.class);
    }

    @Test
    @DisplayName("다른 제약 위반은 중복 신고로 바꾸지 않고 그대로 전파한다")
    void createReport_OtherConstraintViolation_Propagates() {
        when(redisChatRepository.findMessagesByRoomId(ROOM_ID))
                .thenReturn(List.of(message("target", 2L, 0, MessageType.CHAT)));
        ConstraintViolationException cause = new ConstraintViolationException(
                "fk violation", new SQLException("fk"), "fk_report_target_member");
        DataIntegrityViolationException thrown = new DataIntegrityViolationException("fk", cause);
        when(reportRepository.saveAndFlush(any())).thenThrow(thrown);

        assertThatThrownBy(() -> chatReportService.createReport(request("target")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isSameAs(thrown);
    }
}
