package com.dongsoop.dongsoop.memberdevice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.controller.MemberDeviceController;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceResponse;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.memberdevice.util.DeviceUtil;
import com.dongsoop.dongsoop.notification.service.FCMService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
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
    private DeviceBlacklistService deviceBlacklistService;
    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;
    @MockitoBean
    private DeviceUtil deviceUtil;

    private static final Long MEMBER_ID = 1L;
    private static final Long DEVICE_ID = 10L;
    private static final String TOKEN_A = "token-device-a";
    private static final String TOKEN_NEW = "token-device-new";

    // ──────────── GET /device/list ────────────

    @Test
    @DisplayName("X-Device-Token 헤더가 없으면 전체 current가 false로 반환된다")
    void returns_all_current_false_when_header_is_absent() throws Exception {
        given(memberService.getMemberIdByAuthentication()).willReturn(MEMBER_ID);
        given(memberDeviceService.getDeviceList(MEMBER_ID, null)).willReturn(List.of(
                new MemberDeviceResponse(1L, MemberDeviceType.ANDROID, false, null),
                new MemberDeviceResponse(2L, MemberDeviceType.IOS, false, null)
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
                new MemberDeviceResponse(1L, MemberDeviceType.ANDROID, true, LocalDateTime.of(2025, 1, 1, 12, 0)),
                new MemberDeviceResponse(2L, MemberDeviceType.IOS, false, LocalDateTime.of(2025, 1, 1, 10, 0))
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
                new MemberDeviceResponse(1L, MemberDeviceType.ANDROID, false, null),
                new MemberDeviceResponse(2L, MemberDeviceType.IOS, false, null)
        ));

        mockMvc.perform(get("/device/list")
                        .header("X-Device-Token", "unknown-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].current").value(false))
                .andExpect(jsonPath("$[1].current").value(false));
    }

    // ──────────── POST /device ────────────

    @Test
    @DisplayName("JWT에 deviceId가 없으면 새 디바이스를 등록하고 anonymous 토픽을 구독한다")
    void registers_new_device_and_subscribes_anonymous_when_no_existing_device_id() throws Exception {
        given(deviceUtil.getDeviceIdFromContext()).willReturn(null);

        mockMvc.perform(post("/device")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceToken\":\"" + TOKEN_NEW + "\",\"type\":\"ANDROID\"}"))
                .andExpect(status().isCreated());

        verify(memberDeviceService).registerDevice(TOKEN_NEW, MemberDeviceType.ANDROID, null);
        verify(fcmService).subscribeTopic(anyList(), anyString());
    }

    @Test
    @DisplayName("JWT에 deviceId가 있으면 기존 디바이스 토큰을 갱신하고 anonymous 구독을 생략한다")
    void updates_existing_device_token_and_skips_subscribe_when_device_id_present() throws Exception {
        given(deviceUtil.getDeviceIdFromContext()).willReturn(DEVICE_ID);

        mockMvc.perform(post("/device")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceToken\":\"" + TOKEN_NEW + "\",\"type\":\"ANDROID\"}"))
                .andExpect(status().isCreated());

        verify(memberDeviceService).registerDevice(TOKEN_NEW, MemberDeviceType.ANDROID, DEVICE_ID);
        verifyNoInteractions(fcmService);
    }

    // ──────────── DELETE /device/{deviceId} ────────────

    @Test
    @DisplayName("강제 로그아웃 시 디바이스 블랙리스트 등록 후 unbindDevice를 호출한다")
    void force_logout_blacklists_device_and_calls_unbind() throws Exception {
        given(memberService.getMemberIdByAuthentication()).willReturn(MEMBER_ID);
        given(memberDeviceService.getDeviceTokenIfOwned(MEMBER_ID, DEVICE_ID)).willReturn(TOKEN_A);

        mockMvc.perform(delete("/device/{deviceId}", DEVICE_ID))
                .andExpect(status().isNoContent());

        verify(deviceBlacklistService).blacklist(DEVICE_ID);
        verify(memberDeviceService).unbindDevice(DEVICE_ID);
    }

    @Test
    @DisplayName("강제 로그아웃 시 디바이스 토큰이 null이어도 블랙리스트 등록과 unbindDevice는 실행된다")
    void force_logout_proceeds_even_when_device_token_is_null() throws Exception {
        given(memberService.getMemberIdByAuthentication()).willReturn(MEMBER_ID);
        given(memberDeviceService.getDeviceTokenIfOwned(MEMBER_ID, DEVICE_ID)).willReturn(null);

        mockMvc.perform(delete("/device/{deviceId}", DEVICE_ID))
                .andExpect(status().isNoContent());

        verify(deviceBlacklistService).blacklist(DEVICE_ID);
        verify(memberDeviceService).unbindDevice(DEVICE_ID);
    }
}