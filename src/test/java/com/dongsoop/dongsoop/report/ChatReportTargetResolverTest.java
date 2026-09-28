package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatMessageEntity;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.repository.ChatMessageJpaRepository;
import com.dongsoop.dongsoop.chat.service.ChatMessageService;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.report.dto.CreateChatReportRequest;
import com.dongsoop.dongsoop.report.dto.MessageReportDraft;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.NotReportableMessageException;
import com.dongsoop.dongsoop.report.exception.ReportTargetNotFoundException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import com.dongsoop.dongsoop.report.service.ChatReportTargetResolver;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
class ChatReportTargetResolverTest {

    private static final String ROOM_ID = "room1";
    private static final Long REPORTER_ID = 1L;
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 9, 28, 21, 0);

    @InjectMocks
    private ChatReportTargetResolver resolver;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private ChatMessageService chatMessageService;
    @Mock
    private ChatMessageJpaRepository chatMessageJpaRepository;

    private final Member sender = Member.builder().id(2L).build();

    @BeforeEach
    void setUp() {
        when(chatRoomService.getChatRoomById(ROOM_ID)).thenReturn(ChatRoom.builder()
                .roomId(ROOM_ID)
                .participants(new HashSet<>(Set.of(1L, 2L)))
                .build());
        when(memberRepository.findById(2L)).thenReturn(Optional.of(sender));
    }

    private static ChatMessage message(String id, Long senderId, int minute, MessageType type) {
        return ChatMessage.builder().messageId(id).roomId(ROOM_ID).senderId(senderId)
                .content("내용" + id).timestamp(BASE.plusMinutes(minute)).type(type).build();
    }

    private static CreateChatReportRequest request(String messageId) {
        return new CreateChatReportRequest(ROOM_ID, messageId, ReportReason.HATE_SPEECH, "욕함");
    }

    @Test
    @DisplayName("메시지 내용과 직전 사용자 메시지 최대 10개를 복사한다")
    void resolve_CopiesSnapshotAndContext() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(message("enter", 2L, 0, MessageType.ENTER));
        for (int i = 1; i <= 12; i++) {
            messages.add(message("m" + i, i % 2 == 0 ? 2L : 1L, i, MessageType.CHAT));
        }
        messages.add(message("target", 2L, 13, MessageType.CHAT));
        messages.add(message("after", 1L, 14, MessageType.CHAT));
        when(chatMessageService.getAllMessages(ROOM_ID)).thenReturn(messages);

        MessageReportDraft draft = resolver.resolve(REPORTER_ID, request("target"));

        assertThat(draft.reportType()).isEqualTo(ReportType.CHAT_MESSAGE);
        assertThat(draft.targetMember()).isEqualTo(sender);
        assertThat(draft.chatRoomId()).isEqualTo(ROOM_ID);
        assertThat(draft.messageId()).isEqualTo("target");
        assertThat(draft.messageContent()).isEqualTo("내용target");
        assertThat(draft.messageSentAt()).isEqualTo(BASE.plusMinutes(13));
        assertThat(draft.targetUrl()).isEqualTo("/chat/room/" + ROOM_ID);
        assertThat(draft.messageContext().messages())
                .extracting(snapshot -> snapshot.content())
                .containsExactly("내용m3", "내용m4", "내용m5", "내용m6", "내용m7",
                        "내용m8", "내용m9", "내용m10", "내용m11", "내용m12");
    }

    @Test
    @DisplayName("Redis에 없는 메시지는 DB 백업에서 찾는다")
    void resolve_FallsBackToDatabase() {
        when(chatMessageService.getAllMessages(ROOM_ID)).thenReturn(List.of());
        when(chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(ROOM_ID)).thenReturn(List.of(
                ChatMessageEntity.builder().messageId("old").roomId(ROOM_ID).senderId(2L)
                        .content("옛날 욕").timestamp(BASE).type(MessageType.CHAT).build()));

        assertThat(resolver.resolve(REPORTER_ID, request("old")).messageContent()).isEqualTo("옛날 욕");
    }

    @Test
    @DisplayName("타입이 비어 있는 과거 메시지는 사용자 메시지로 보고 신고를 받는다")
    void resolve_NullTypeIsUserMessage() {
        when(chatMessageService.getAllMessages(ROOM_ID)).thenReturn(List.of(message("legacy", 2L, 0, null)));

        assertThat(resolver.resolve(REPORTER_ID, request("legacy")).messageId()).isEqualTo("legacy");
    }

    @Test
    @DisplayName("방 참여자가 아니면 신고할 수 없다")
    void resolve_NotParticipant_Throws() {
        assertThatThrownBy(() -> resolver.resolve(3L, request("target")))
                .isInstanceOf(UnauthorizedChatAccessException.class);
    }

    @Test
    @DisplayName("방에 없는 메시지는 찾을 수 없다")
    void resolve_UnknownMessage_Throws() {
        when(chatMessageService.getAllMessages(ROOM_ID)).thenReturn(List.of());
        when(chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(ROOM_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> resolver.resolve(REPORTER_ID, request("nope")))
                .isInstanceOf(ReportTargetNotFoundException.class);
    }

    @Test
    @DisplayName("입장·퇴장 시스템 메시지는 신고할 수 없다")
    void resolve_SystemMessage_Throws() {
        when(chatMessageService.getAllMessages(ROOM_ID)).thenReturn(List.of(message("leave", 2L, 0, MessageType.LEAVE)));

        assertThatThrownBy(() -> resolver.resolve(REPORTER_ID, request("leave")))
                .isInstanceOf(NotReportableMessageException.class);
    }

    @Test
    @DisplayName("자기 메시지는 신고할 수 없다")
    void resolve_OwnMessage_Throws() {
        when(chatMessageService.getAllMessages(ROOM_ID)).thenReturn(List.of(message("mine", 1L, 0, MessageType.CHAT)));

        assertThatThrownBy(() -> resolver.resolve(REPORTER_ID, request("mine")))
                .isInstanceOf(SelfReportException.class);
    }
}
