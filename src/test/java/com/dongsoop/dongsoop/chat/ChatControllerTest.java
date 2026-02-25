package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.chat.controller.ChatController;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ChatService chatService;
    @MockitoBean
    private ChatRoomService chatRoomService;
    @MockitoBean
    private MemberService memberService;
    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;

    private void mockCurrentUser() {
        Member member = Member.builder().id(1L).build();
        when(memberService.getMemberReferenceByContext()).thenReturn(member);
    }

    @Test
    @DisplayName("POST /chat/room - targetUserId null이면 400")
    void createRoom_invalidRequest_returns400() throws Exception {
        mockCurrentUser();

        mockMvc.perform(post("/chat/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /chat/room - 유효한 요청이면 200")
    void createRoom_validRequest_returns200() throws Exception {
        mockCurrentUser();

        mockMvc.perform(post("/chat/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\": 2, \"title\": \"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /chat/room/group - participants null이면 400")
    void createGroupRoom_nullParticipants_returns400() throws Exception {
        mockCurrentUser();

        mockMvc.perform(post("/chat/room/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"group\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /chat/room/group - 빈 participants면 400")
    void createGroupRoom_emptyParticipants_returns400() throws Exception {
        mockCurrentUser();

        mockMvc.perform(post("/chat/room/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participants\": [], \"title\": \"group\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /chat/room/{roomId}/kick - userId null이면 400")
    void kickUser_nullUserId_returns400() throws Exception {
        mockCurrentUser();

        mockMvc.perform(post("/chat/room/room1/kick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /chat/room/contact - targetUserId null이면 400")
    void createContactRoom_nullTargetUserId_returns400() throws Exception {
        mockCurrentUser();

        mockMvc.perform(post("/chat/room/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardType\": \"PROJECT\", \"boardId\": 1, \"boardTitle\": \"test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /chat/room/contact - boardTitle 빈 값이면 400")
    void createContactRoom_blankBoardTitle_returns400() throws Exception {
        mockCurrentUser();

        mockMvc.perform(post("/chat/room/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\": 2, \"boardType\": \"PROJECT\", \"boardId\": 1, \"boardTitle\": \"\"}"))
                .andExpect(status().isBadRequest());
    }
}
