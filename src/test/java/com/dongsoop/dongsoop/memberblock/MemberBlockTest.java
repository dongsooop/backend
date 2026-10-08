package com.dongsoop.dongsoop.memberblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.dongsoop.dongsoop.memberblock.service.MemberBlockService;
import com.dongsoop.dongsoop.memberblock.service.MemberBlockServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
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
class MemberBlockTest {

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
    private MemberBlockService memberBlockService;
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
    @DisplayName("멤버 차단 정보가 DB에 저장된다.")
    void blockedMember_WhenMemberRequest_SavedDataBase() throws Exception {
        // given
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);
        when(memberRepository.existsById(2L)).thenReturn(true);
        when(memberRepository.getReferenceById(any(Long.class)))
                .thenAnswer(invocation -> Member.builder()
                        .id(invocation.getArgument(0, Long.class))
                        .build());

        Member blocker = Member.builder()
                .id(1L)
                .build();

        Member blockedMember = Member.builder()
                .id(2L)
                .build();

        MemberBlock answer = new MemberBlock(new MemberBlockId(blocker, blockedMember));

        // when
        MockHttpServletRequestBuilder request = post("/member-block")
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
        verify(memberBlockRepository).save(captor.capture());

        MemberBlock memberBlock = captor.getValue();
        assertThat(memberBlock)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(answer);
    }

    @Test
    @DisplayName("자기 자신은 차단할 수 없다")
    void block_WhenSelf_ReturnsBadRequest() throws Exception {
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);

        mockMvc.perform(post("/member-block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "blockedMemberId": 1 }
                                """))
                .andExpect(status().isBadRequest());

        verify(memberBlockRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 회원은 차단할 수 없다")
    void block_WhenTargetMissing_ReturnsNotFound() throws Exception {
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);
        when(memberRepository.existsById(2L)).thenReturn(false);

        mockMvc.perform(post("/member-block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "blockedMemberId": 2 }
                                """))
                .andExpect(status().isNotFound());

        verify(memberBlockRepository, never()).save(any());
    }

    @Test
    @DisplayName("차단 후 두 사람에게 현재 차단 상태를 다시 계산해 보낸다")
    void block_SendsRecalculatedStatusToBoth() throws Exception {
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);
        when(memberRepository.existsById(2L)).thenReturn(true);
        when(memberRepository.getReferenceById(any(Long.class)))
                .thenAnswer(invocation -> Member.builder().id(invocation.getArgument(0, Long.class)).build());
        ChatRoom room = ChatRoom.builder().roomId("room1").build();
        when(redisChatRepository.findRoomByParticipants(1L, 2L)).thenReturn(Optional.of(room));
        when(chatService.getBlockStatus("room1", 1L)).thenReturn(BlockStatus.I_BLOCKED);
        when(chatService.getBlockStatus("room1", 2L)).thenReturn(BlockStatus.BLOCKED_BY_OTHER);

        mockMvc.perform(post("/member-block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "blockedMemberId": 2 }
                                """))
                .andExpect(status().isNoContent());

        verify(chatService).sendBlockStatusToUser("room1", 1L, BlockStatus.I_BLOCKED);
        verify(chatService).sendBlockStatusToUser("room1", 2L, BlockStatus.BLOCKED_BY_OTHER);
    }

    @Test
    @DisplayName("동시 차단 요청으로 키 중복이 나면 이미 차단한 유저로 400을 준다")
    void block_WhenConcurrentDuplicate_ReturnsBadRequest() throws Exception {
        when(memberService.getMemberIdByAuthentication()).thenReturn(1L);
        when(memberRepository.existsById(2L)).thenReturn(true);
        when(memberRepository.getReferenceById(any(Long.class)))
                .thenAnswer(invocation -> Member.builder().id(invocation.getArgument(0, Long.class)).build());
        when(memberBlockRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        mockMvc.perform(post("/member-block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "blockedMemberId": 2 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("이미 차단한 유저입니다."));

        verify(redisChatRepository, never()).findRoomByParticipants(any(), any());
    }
}
