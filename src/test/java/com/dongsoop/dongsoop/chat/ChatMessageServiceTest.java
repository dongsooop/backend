package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatMessageService;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private RedisChatRepository redisChatRepository;
    @Mock
    private ChatValidator chatValidator;
    @Mock
    private MemberService memberService;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("processMessage - enriched 메시지가 저장된다")
    void processMessage_savesEnrichedMessage() {
        ChatMessage original = ChatMessage.builder()
                .roomId("room1")
                .senderId(1L)
                .content("hello")
                .build();

        ChatMessage enriched = ChatMessage.builder()
                .messageId("enriched-id")
                .roomId("room1")
                .senderId(1L)
                .content("hello")
                .timestamp(LocalDateTime.now())
                .type(MessageType.CHAT)
                .build();

        when(chatValidator.validateAndEnrichMessage(any())).thenReturn(enriched);

        ChatMessage result = chatMessageService.processMessage(original);

        verify(redisChatRepository).saveMessage(enriched);
        assertThat(result).isEqualTo(enriched);
    }

    @Test
    @DisplayName("createAndSaveSystemMessage - LEAVE 시스템 메시지 생성")
    void createAndSaveSystemMessage_leave() {
        when(memberService.getNicknameById(1L)).thenReturn("테스트유저");

        ChatMessage result = chatMessageService.createAndSaveSystemMessage("room1", 1L, MessageType.LEAVE);

        assertThat(result.getContent()).contains("테스트유저님이 나갔습니다.");
        assertThat(result.getType()).isEqualTo(MessageType.LEAVE);
        verify(redisChatRepository).saveMessage(any(ChatMessage.class));
    }

    @Test
    @DisplayName("createAndSaveSystemMessage - ENTER 시스템 메시지 생성")
    void createAndSaveSystemMessage_enter() {
        when(memberService.getNicknameById(1L)).thenReturn("테스트유저");

        ChatMessage result = chatMessageService.createAndSaveSystemMessage("room1", 1L, MessageType.ENTER);

        assertThat(result.getContent()).contains("테스트유저님이 입장했습니다.");
        assertThat(result.getType()).isEqualTo(MessageType.ENTER);
    }

    @Test
    @DisplayName("getLastMessageText - 마지막 메시지 텍스트 반환")
    void getLastMessageText_returnsContent() {
        ChatMessage lastMsg = ChatMessage.builder().content("마지막 메시지").build();
        when(redisChatRepository.findLastMessageByRoomId("room1")).thenReturn(lastMsg);

        String result = chatMessageService.getLastMessageText("room1");

        assertThat(result).isEqualTo("마지막 메시지");
    }

    @Test
    @DisplayName("getLastMessageText - 메시지 없으면 null")
    void getLastMessageText_returnsNullWhenNoMessages() {
        when(redisChatRepository.findLastMessageByRoomId("room1")).thenReturn(null);

        String result = chatMessageService.getLastMessageText("room1");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getLastMessageTextsBatch - 여러 방의 마지막 메시지를 일괄 조회")
    void getLastMessageTextsBatch_returnsMap() {
        ChatMessage msg1 = ChatMessage.builder().content("msg1").build();
        ChatMessage msg2 = ChatMessage.builder().content("msg2").build();

        when(redisChatRepository.findLastMessagesByRoomIds(List.of("room1", "room2")))
                .thenReturn(Map.of("room1", msg1, "room2", msg2));

        Map<String, String> result = chatMessageService.getLastMessageTextsBatch(List.of("room1", "room2"));

        assertThat(result).hasSize(2);
        assertThat(result.get("room1")).isEqualTo("msg1");
        assertThat(result.get("room2")).isEqualTo("msg2");
    }

    @Test
    @DisplayName("countUnreadMessages - 자신의 메시지를 제외한 안읽은 메시지 수")
    void countUnreadMessages_excludesSelf() {
        List<ChatMessage> messages = List.of(
                ChatMessage.builder().senderId(1L).build(),
                ChatMessage.builder().senderId(2L).build(),
                ChatMessage.builder().senderId(2L).build()
        );

        int result = chatMessageService.countUnreadMessages(messages, 1L);

        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("findMessageById - 메시지 찾기 성공")
    void findMessageById_found() {
        ChatMessage target = ChatMessage.builder().messageId("m1").build();
        List<ChatMessage> messages = List.of(
                ChatMessage.builder().messageId("m0").build(),
                target
        );

        ChatMessage result = chatMessageService.findMessageById(messages, "m1");

        assertThat(result).isEqualTo(target);
    }

    @Test
    @DisplayName("findMessageById - 메시지 없으면 null")
    void findMessageById_notFound() {
        List<ChatMessage> messages = List.of(
                ChatMessage.builder().messageId("m0").build()
        );

        ChatMessage result = chatMessageService.findMessageById(messages, "unknown");

        assertThat(result).isNull();
    }
}
