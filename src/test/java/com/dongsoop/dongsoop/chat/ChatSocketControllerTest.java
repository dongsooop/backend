package com.dongsoop.dongsoop.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.controller.ChatSocketController;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.memberblock.constant.BlockStatus;
import java.security.Principal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ChatSocketControllerTest {

    @InjectMocks
    private ChatSocketController chatSocketController;

    @Mock
    private ChatService chatService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final Principal principal = () -> "2";

    @Test
    @DisplayName("상대에게 차단당한 사용자는 메시지를 보낼 수 없다")
    void sendMessage_WhenBlockedByOther_DropsMessage() {
        when(chatService.getBlockStatus("room1", 2L)).thenReturn(BlockStatus.BLOCKED_BY_OTHER);

        chatSocketController.sendMessage(ChatMessage.builder().content("안녕").build(), "room1", principal);

        verify(chatService, never()).processWebSocketMessage(any(), any(), any());
    }

    @Test
    @DisplayName("차단 관계가 없으면 메시지를 처리한다")
    void sendMessage_WhenNoBlock_ProcessesMessage() {
        ChatMessage message = ChatMessage.builder().content("안녕").build();
        when(chatService.getBlockStatus("room1", 2L)).thenReturn(BlockStatus.NONE);

        chatSocketController.sendMessage(message, "room1", principal);

        verify(chatService).processWebSocketMessage(eq(message), eq(2L), eq("room1"));
    }
}
