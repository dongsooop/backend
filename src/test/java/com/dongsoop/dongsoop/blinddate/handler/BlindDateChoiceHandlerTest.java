package com.dongsoop.dongsoop.blinddate.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateMatchNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class BlindDateChoiceHandlerTest {
    private final BlindDateParticipantStorageImpl participants = new BlindDateParticipantStorageImpl();
    private final BlindDateSessionStorageImpl sessions = new BlindDateSessionStorageImpl(event -> {});
    private final ChatRoomService rooms = mock(ChatRoomService.class);
    private final BlindDateEventQueue queue = new BlindDateEventQueue();
    private final List<ChatRoom> created = new CopyOnWriteArrayList<>();
    private final BlindDateChoiceHandler handler = new BlindDateChoiceHandler(
            participants, sessions, mock(SimpMessagingTemplate.class), rooms, queue, mock(BlindDateMatchNotification.class));

    @AfterEach
    void tearDown() {
        queue.shutdown();
    }

    @Test
    void mutualChoiceCreatesRoomWithBlindDatePrefixAndKstDate() {
        String id = sessions.create().getSessionId();
        sessions.start(id);
        participants.addParticipant(id, 1L, "one");
        participants.addParticipant(id, 2L, "two");
        participants.openChoices(id);
        queue.openChoices(id);
        when(rooms.createOneToOneChatRoom(anyLong(), anyLong(), anyString())).thenAnswer(call -> {
            ChatRoom room = ChatRoom.builder().roomId("room").title(call.getArgument(2)).build();
            created.add(room);
            return room;
        });
        LocalDate before = LocalDate.now(ZoneId.of("Asia/Seoul"));
        handler.execute(id, 1L, 2L);
        handler.execute(id, 2L, 1L);
        queue.awaitIdle();
        LocalDate after = LocalDate.now(ZoneId.of("Asia/Seoul"));
        assertThat(created).hasSize(1);
        assertThat(created.get(0).getTitle()).isIn("[과팅] " + before, "[과팅] " + after);
    }
}
