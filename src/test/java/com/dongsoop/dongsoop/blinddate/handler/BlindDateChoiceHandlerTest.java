package com.dongsoop.dongsoop.blinddate.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlindDateChoiceHandler 단위 테스트")
class BlindDateChoiceHandlerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private BlindDateParticipantStorage participantStorage;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ChatRoomService chatRoomService;

    private BlindDateChoiceHandler handler;

    @BeforeEach
    void setUp() {
        handler = new BlindDateChoiceHandler(participantStorage, messagingTemplate, chatRoomService);
    }

    @Test
    @DisplayName("매칭 채팅방 제목은 KST 기준 yyyy-MM-dd 형식이다")
    void matchedChatRoom_UsesDateOnlyTitle() {
        when(participantStorage.recordChoice("session-1", 1L, 2L)).thenReturn(true);
        when(chatRoomService.createOneToOneChatRoom(eq(1L), eq(2L),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(ChatRoom.builder().roomId("room-1").title("title").build());

        handler.execute("session-1", 1L, 2L);

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatRoomService).createOneToOneChatRoom(eq(1L), eq(2L), titleCaptor.capture());
        assertThat(titleCaptor.getValue())
                .isEqualTo(LocalDate.now(KST).toString())
                .matches("\\d{4}-\\d{2}-\\d{2}");
    }
}
