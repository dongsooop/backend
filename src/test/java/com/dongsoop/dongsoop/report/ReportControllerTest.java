package com.dongsoop.dongsoop.report;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.memberdevice.util.DeviceUtil;
import com.dongsoop.dongsoop.report.controller.ReportController;
import com.dongsoop.dongsoop.report.dto.CreateBlindDateReportRequest;
import com.dongsoop.dongsoop.report.dto.ReportContextResponse;
import com.dongsoop.dongsoop.report.dto.ReportContextResponse.AfterSource;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.exception.ReportNotFoundException;
import com.dongsoop.dongsoop.report.dto.SanctionStatusResponse;
import com.dongsoop.dongsoop.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;
    @MockitoBean
    private DeviceUtil deviceUtil;
    @MockitoBean
    private MemberDeviceService memberDeviceService;

    @Test
    @DisplayName("제재당하지 않은 사용자의 제재 상태 확인 시 정상 응답을 반환한다")
    void checkSanctionStatus_WhenNotSanctioned_ShouldReturnNormalStatus() throws Exception {
        // given
        SanctionStatusResponse response = new SanctionStatusResponse(
                false, null, null, null, null, null
        );
        when(reportService.checkAndUpdateSanctionStatus()).thenReturn(response);

        // when & then
        mockMvc.perform(get("/reports/sanction-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSanctioned").value(false))
                .andExpect(jsonPath("$.sanctionType").isEmpty())
                .andExpect(jsonPath("$.reason").isEmpty())
                .andExpect(jsonPath("$.startDate").isEmpty())
                .andExpect(jsonPath("$.endDate").isEmpty())
                .andExpect(jsonPath("$.description").isEmpty());
    }

    @Test
    @DisplayName("제재당한 사용자의 제재 상태 확인 시 제재 정보를 반환한다")
    void checkSanctionStatus_WhenSanctioned_ShouldReturnSanctionInfo() throws Exception {
        // given
        LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 1, 31, 23, 59);

        SanctionStatusResponse response = new SanctionStatusResponse(
                true,
                "TEMPORARY_BAN",
                "부적절한 게시글 작성",
                startDate,
                endDate,
                "30일 임시 정지 처분"
        );
        when(reportService.checkAndUpdateSanctionStatus()).thenReturn(response);

        // when & then
        mockMvc.perform(get("/reports/sanction-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSanctioned").value(true))
                .andExpect(jsonPath("$.sanctionType").value("TEMPORARY_BAN"))
                .andExpect(jsonPath("$.reason").value("부적절한 게시글 작성"))
                .andExpect(jsonPath("$.startDate").value("2025-01-01T00:00:00"))
                .andExpect(jsonPath("$.endDate").value("2025-01-31T23:59:00"))
                .andExpect(jsonPath("$.description").value("30일 임시 정지 처분"));
    }

    @Test
    @DisplayName("채팅 신고 요청은 201을 반환한다")
    void createChatReport_ReturnsCreated() throws Exception {
        mockMvc.perform(post("/reports/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "roomId": "room1", "messageId": "m1", "reason": "HATE_SPEECH" }
                                """))
                .andExpect(status().isCreated());

        verify(reportService).createChatReport(any());
    }

    @Test
    @DisplayName("채팅 신고에 메시지 ID가 없으면 400을 반환한다")
    void createChatReport_WithoutMessageId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/reports/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "roomId": "room1", "reason": "HATE_SPEECH" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("과팅 신고 요청은 서비스로 넘겨 201을 반환한다")
    void createBlindDateReport_ReturnsCreated() throws Exception {
        mockMvc.perform(post("/reports/blinddate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": "session-1", "messageId": "m1", "reason": "HATE_SPEECH",
                                  "description": "욕설" }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateBlindDateReportRequest> captor =
                ArgumentCaptor.forClass(CreateBlindDateReportRequest.class);
        verify(reportService).createBlindDateReport(captor.capture());
        assertThat(captor.getValue().sessionId()).isEqualTo("session-1");
        assertThat(captor.getValue().messageId()).isEqualTo("m1");
        assertThat(captor.getValue().description()).isEqualTo("욕설");
    }

    @Test
    @DisplayName("과팅 신고에 세션 ID가 없으면 400을 반환하고 서비스를 부르지 않는다")
    void createBlindDateReport_WithoutSessionId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/reports/blinddate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "messageId": "m1", "reason": "HATE_SPEECH" }
                                """))
                .andExpect(status().isBadRequest());

        verify(reportService, never()).createBlindDateReport(any());
    }

    @Test
    @DisplayName("과팅 신고 내용이 500자를 넘으면 400을 반환한다")
    void createBlindDateReport_TooLongDescription_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/reports/blinddate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sessionId": "session-1", "messageId": "m1", "reason": "HATE_SPEECH",
                                  "description": "%s" }
                                """.formatted("가".repeat(501))))
                .andExpect(status().isBadRequest());

        verify(reportService, never()).createBlindDateReport(any());
    }

    @Test
    @DisplayName("신고 기각은 204를 반환한다")
    void dismissReport_ReturnsNoContent() throws Exception {
        mockMvc.perform(post("/reports/7/dismiss"))
                .andExpect(status().isNoContent());

        verify(reportService).dismissReport(7L);
    }

    @Test
    @DisplayName("신고 맥락 조회는 원문·앞뒤 맥락·뒤쪽 출처를 반환한다")
    void getReportContext_ReturnsContext() throws Exception {
        LocalDateTime sentAt = LocalDateTime.of(2026, 9, 28, 21, 0);
        when(reportService.getReportContext(7L)).thenReturn(new ReportContextResponse("m1", "욕설",
                List.of(ChatMessageSnapshot.of(1L, "앞말", sentAt)),
                List.of(ChatMessageSnapshot.of(2L, "뒷말", sentAt.plusMinutes(1))), AfterSource.SAVED));

        mockMvc.perform(get("/reports/7/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("m1"))
                .andExpect(jsonPath("$.messageContent").value("욕설"))
                .andExpect(jsonPath("$.before[0].senderId").value(1))
                .andExpect(jsonPath("$.before[0].content").value("앞말"))
                .andExpect(jsonPath("$.after[0].content").value("뒷말"))
                .andExpect(jsonPath("$.after[0].sentAt").exists())
                .andExpect(jsonPath("$.afterSource").value("SAVED"));
    }

    @Test
    @DisplayName("없는 신고의 맥락 조회는 404를 반환한다")
    void getReportContext_Missing_ReturnsNotFound() throws Exception {
        when(reportService.getReportContext(9L)).thenThrow(new ReportNotFoundException(9L));

        mockMvc.perform(get("/reports/9/context"))
                .andExpect(status().isNotFound());
    }
}
