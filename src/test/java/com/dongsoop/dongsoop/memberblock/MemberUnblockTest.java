package com.dongsoop.dongsoop.memberblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.memberdevice.util.DeviceUtil;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberblock.constant.BlockStatus;
import com.dongsoop.dongsoop.memberblock.controller.MemberBlockController;
import com.dongsoop.dongsoop.memberblock.entity.MemberBlock;
import com.dongsoop.dongsoop.memberblock.entity.MemberBlockId;
import com.dongsoop.dongsoop.memberblock.repository.MemberBlockRepository;
import com.dongsoop.dongsoop.memberblock.repository.MemberBlockRepositoryCustom;
import com.dongsoop.dongsoop.memberblock.service.MemberBlockServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = MemberBlockController.class)
@Import(MemberBlockServiceImpl.class)
@AutoConfigureMockMvc(addFilters = false)
public class MemberUnblockTest {

    @MockitoBean
    private MemberService memberService;
    @MockitoBean
    private MemberRepository memberRepository;
    @MockitoBean
    private MemberBlockRepository memberBlockRepository;
    @MockitoBean
    private MemberBlockRepositoryCustom memberBlockRepositoryCustom;
    @MockitoBean
    private ChatService chatService;
    @MockitoBean
    private RedisChatRepository redisChatRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;
    @MockitoBean
    private DeviceUtil deviceUtil;
    @MockitoBean
    private MemberDeviceService memberDeviceService;

    @Test
    @DisplayName("DB에 저장된 멤버 차단 정보를 제거한다.")
    void unblockedMember_WhenMemberRequest_SavedDataBase() throws Exception {
        // given
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);
        when(memberRepository.getReferenceById(any(Long.class)))
                .thenAnswer(invocation -> Member.builder()
                        .id(invocation.getArgument(0, Long.class))
                        .build());

        when(memberBlockRepository.existsById(any(MemberBlockId.class)))
                .thenReturn(true);

        Member blocker = Member.builder()
                .id(1L)
                .build();

        Member blockedMember = Member.builder()
                .id(2L)
                .build();

        MemberBlock answer = new MemberBlock(new MemberBlockId(blocker, blockedMember));

        when(memberBlockRepository.getReferenceById(any(MemberBlockId.class)))
                .thenReturn(answer);

        // when
        MockHttpServletRequestBuilder request = delete("/member-block")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "blockerId": 999,
                            "blockedMemberId": 2
                        }
                        """);

        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        // then
        ArgumentCaptor<MemberBlock> captor = ArgumentCaptor.forClass(MemberBlock.class);
        verify(memberBlockRepository).delete(captor.capture());

        MemberBlock memberBlock = captor.getValue();
        assertThat(memberBlock)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(answer);
    }

    @Test
    @DisplayName("해제 후 상대에게도 다시 계산한 상태(NONE)를 보낸다")
    void unblock_SendsRecalculatedStatusToBoth() throws Exception {
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);
        when(memberRepository.getReferenceById(any(Long.class)))
                .thenAnswer(invocation -> Member.builder().id(invocation.getArgument(0, Long.class)).build());
        when(memberBlockRepository.existsById(any(MemberBlockId.class))).thenReturn(true);
        ChatRoom room = ChatRoom.builder().roomId("room1").build();
        when(redisChatRepository.findRoomByParticipants(1L, 2L)).thenReturn(Optional.of(room));
        when(chatService.getBlockStatus("room1", 1L)).thenReturn(BlockStatus.NONE);
        when(chatService.getBlockStatus("room1", 2L)).thenReturn(BlockStatus.NONE);

        mockMvc.perform(delete("/member-block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "blockedMemberId": 2 }
                                """))
                .andExpect(status().isNoContent());

        verify(chatService).sendBlockStatusToUser("room1", 2L, BlockStatus.NONE);
    }

    @Test
    @DisplayName("대상 ID 없이 해제하면 404")
    void unblock_WhenTargetMissing_ReturnsNotFound() throws Exception {
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);

        mockMvc.perform(delete("/member-block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        verify(memberBlockRepository, never()).delete(any());
    }
}
