package com.dongsoop.dongsoop.report;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.report.controller.ReportController;
import com.dongsoop.dongsoop.report.dto.SanctionStatusResponse;
import com.dongsoop.dongsoop.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
