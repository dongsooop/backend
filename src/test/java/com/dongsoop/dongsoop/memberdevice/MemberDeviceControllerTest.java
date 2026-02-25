package com.dongsoop.dongsoop.memberdevice;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.controller.MemberDeviceController;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceResponse;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MemberDeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberDeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberDeviceService memberDeviceService;
    @MockitoBean
    private MemberService memberService;
    @MockitoBean
    private FCMService fcmService;
    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;

    private static final Long MEMBER_ID = 1L;
    private static final String TOKEN_A = "token-device-a";

    @Test
    @DisplayName("X-Device-Token 헤더가 없으면 전체 current가 false로 반환된다")
    void returns_all_current_false_when_header_is_absent() throws Exception {
        given(memberService.getMemberIdByAuthentication()).willReturn(MEMBER_ID);
        given(memberDeviceService.getDeviceList(MEMBER_ID, null)).willReturn(List.of(
                new MemberDeviceResponse(1L, MemberDeviceType.ANDROID, false),
                new MemberDeviceResponse(2L, MemberDeviceType.IOS, false)
        ));

        mockMvc.perform(get("/device/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].current").value(false))
                .andExpect(jsonPath("$[1].current").value(false));
    }

    @Test
    @DisplayName("X-Device-Token 헤더가 일치하는 기기의 current가 true로 반환된다")
    void returns_current_true_for_matching_device() throws Exception {
        given(memberService.getMemberIdByAuthentication()).willReturn(MEMBER_ID);
        given(memberDeviceService.getDeviceList(MEMBER_ID, TOKEN_A)).willReturn(List.of(
                new MemberDeviceResponse(1L, MemberDeviceType.ANDROID, true),
                new MemberDeviceResponse(2L, MemberDeviceType.IOS, false)
        ));

        mockMvc.perform(get("/device/list")
                        .header("X-Device-Token", TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].current").value(true))
                .andExpect(jsonPath("$[1].current").value(false));
    }

    @Test
    @DisplayName("X-Device-Token 헤더가 일치하지 않으면 전체 current가 false로 반환된다")
    void returns_all_current_false_when_token_does_not_match() throws Exception {
        given(memberService.getMemberIdByAuthentication()).willReturn(MEMBER_ID);
        given(memberDeviceService.getDeviceList(MEMBER_ID, "unknown-token")).willReturn(List.of(
                new MemberDeviceResponse(1L, MemberDeviceType.ANDROID, false),
                new MemberDeviceResponse(2L, MemberDeviceType.IOS, false)
        ));

        mockMvc.perform(get("/device/list")
                        .header("X-Device-Token", "unknown-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].current").value(false))
                .andExpect(jsonPath("$[1].current").value(false));
    }
}
