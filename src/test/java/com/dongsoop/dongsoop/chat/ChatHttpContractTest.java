package com.dongsoop.dongsoop.chat;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dongsoop.dongsoop.chat.controller.ChatController;
import com.dongsoop.dongsoop.chat.dto.ReadStatusUpdateRequest;
import com.dongsoop.dongsoop.chat.entity.*;
import com.dongsoop.dongsoop.chat.service.*;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ChatHttpContractTest {
    @Mock ChatService chat;
    @Mock ChatRoomService rooms;
    @Mock ChatParticipantService participants;
    @Mock ChatValidator validator;
    @Mock MemberService members;
    @InjectMocks ChatController controller;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(members.getMemberReferenceByContext()).thenReturn(Member.builder().id(7L).build());
    }

    @Test
    void initializationAndRoomListUseCurrentMember() throws Exception {
        var room = ChatRoom.builder().roomId("room").title("Study").build();
        when(chat.initializeChatRoomForFirstTime("room", 7L)).thenReturn(
                ChatRoomInitResponse.builder().room(room).messages(List.of()).totalMessageCount(3).build());
        when(rooms.getRoomsForUserId(7L)).thenReturn(List.of(room));
        when(chat.buildRoomListResponses(List.of(room), 7L)).thenReturn(List.of());
        mvc.perform(get("/chat/room/room/initialize").param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.room.title").value("Study"))
                .andExpect(jsonPath("$.totalMessageCount").value(3));
        mvc.perform(get("/chat/rooms")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(chat).buildRoomListResponses(List.of(room), 7L);
    }

    @Test
    void readStatusBindsCursorAndUsesServerIdentity() throws Exception {
        when(chat.getUnreadMessageCount("room", 7L)).thenReturn(4);
        mvc.perform(post("/chat/room/room/read-status").contentType(MediaType.APPLICATION_JSON).content("""
                {"readUntilTime":"2026-10-16T12:00:00","lastReadMessageId":"cursor","memberId":999}
                """))
                .andExpect(status().isNoContent());
        verify(chat).updateReadStatus(eq("room"), eq(7L), argThat((ReadStatusUpdateRequest r) ->
                r.getLastReadMessageId().equals("cursor") && r.getReadUntilTime().equals(LocalDateTime.of(2026, 10, 16, 12, 0))));
        mvc.perform(get("/chat/room/room/unread-count")).andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(4));
        mvc.perform(post("/chat/room/room/messages/mark-all-read")).andExpect(status().isNoContent());
        verify(chat).markAllMessagesAsRead("room", 7L);
    }

    @Test
    void cursorAndOfflineSyncReturnProcessedServerMessages() throws Exception {
        var saved = ChatMessage.builder().messageId("saved").roomId("room").senderId(7L).content("hello").build();
        when(chat.getMessagesAfter("room", 7L, "cursor")).thenReturn(List.of(saved));
        when(chat.syncOfflineMessages(eq("room"), eq(7L), anyList())).thenReturn(List.of(saved));
        mvc.perform(get("/chat/room/room/messages/after/cursor")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value("saved"));
        mvc.perform(post("/chat/room/room/messages/sync-offline").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"senderId\":999,\"content\":\"hello\"}]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.processedCount").value(1))
                .andExpect(jsonPath("$.processedMessages[0].senderId").value(7));
        verify(chat).syncOfflineMessages(eq("room"), eq(7L), argThat(messages ->
                messages.size() == 1 && messages.get(0).getContent().equals("hello")));
    }

    @Test
    void participantsValidateRoomAccessBeforeLoadingNicknames() throws Exception {
        when(rooms.getChatRoomById("room")).thenReturn(ChatRoom.builder().participants(Set.of(7L, 8L)).build());
        when(members.getNicknameById(7L)).thenReturn("Requester");
        when(members.getNicknameById(8L)).thenReturn("Partner");
        mvc.perform(get("/chat/room/room/participants")).andExpect(status().isOk())
                .andExpect(jsonPath("$.7").value("Requester")).andExpect(jsonPath("$.8").value("Partner"));
        var order = inOrder(validator, rooms);
        order.verify(validator).validateUserForRoom("room", 7L);
        order.verify(rooms).getChatRoomById("room");
    }

    @Test
    void inviteAndLeaveUseServerRequester() throws Exception {
        when(participants.inviteUserToGroupChat("room", 7L, 8L)).thenReturn(
                ChatMessage.builder().messageId("invite").type(MessageType.ENTER).build());
        mvc.perform(post("/chat/room/room/invite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":8,\"memberId\":999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.messageId").value("invite"));
        mvc.perform(post("/chat/room/room/leave")).andExpect(status().isNoContent());
        verify(chat).leaveChatRoom("room", 7L);
    }
}
