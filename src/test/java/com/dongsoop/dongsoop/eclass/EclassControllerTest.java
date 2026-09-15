package com.dongsoop.dongsoop.eclass;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.eclass.controller.EclassController;
import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentListResponse;
import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentResponse;
import com.dongsoop.dongsoop.eclass.dto.EclassLinkResponse;
import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import com.dongsoop.dongsoop.eclass.service.EclassAssignmentService;
import com.dongsoop.dongsoop.eclass.service.EclassLinkService;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.memberdevice.util.DeviceUtil;
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

@WebMvcTest(controllers = EclassController.class)
@AutoConfigureMockMvc(addFilters = false)
class EclassControllerTest {

    private static final String FID_HEADER = "X-Device-Fid";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EclassLinkService eclassLinkService;
    @MockitoBean
    private EclassAssignmentService eclassAssignmentService;

    @MockitoBean
    private MemberService memberService;
    @MockitoBean
    private FCMService fcmService;
    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;
    @MockitoBean
    private DeviceUtil deviceUtil;
    @MockitoBean
    private MemberDeviceService memberDeviceService;

    @Test
    @DisplayName("연동 요청이 성공하면 연동 정보를 반환한다")
    void link() throws Exception {
        when(eclassLinkService.link(eq("fid-1"), any(), eq("moodle-token")))
                .thenReturn(new EclassLinkResponse(true, EclassLinkStatus.ACTIVE, "백승민", null));

        mockMvc.perform(post("/eclass/link")
                        .header(FID_HEADER, "fid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"moodle-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true))
                .andExpect(jsonPath("$.moodleFullname").value("백승민"));
    }

    @Test
    @DisplayName("토큰이 비어 있으면 400을 반환한다")
    void linkWithBlankToken() throws Exception {
        mockMvc.perform(post("/eclass/link")
                        .header(FID_HEADER, "fid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("연동하지 않은 기기는 linked=false를 받는다")
    void getLinkWhenUnlinked() throws Exception {
        when(eclassLinkService.getStatus(any(), any())).thenReturn(EclassLinkResponse.unlinked());

        mockMvc.perform(get("/eclass/link").header(FID_HEADER, "fid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(false));
    }

    @Test
    @DisplayName("과제 목록은 D-day와 함께 반환된다")
    void getAssignments() throws Exception {
        EclassAssignmentResponse assignment = new EclassAssignmentResponse(1L, 501L, "자료구조", "3주차_과제",
                LocalDateTime.of(2026, 9, 25, 23, 55), null, 3L, false,
                "https://eclass.dongyang.ac.kr/mod/assign/view.php?id=9101");
        when(eclassAssignmentService.getUpcoming(any(), any()))
                .thenReturn(EclassAssignmentListResponse.of(List.of(assignment)));

        mockMvc.perform(get("/eclass/assignments").header(FID_HEADER, "fid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true))
                .andExpect(jsonPath("$.assignments[0].courseName").value("자료구조"))
                .andExpect(jsonPath("$.assignments[0].dDay").value(3));
    }

    @Test
    @DisplayName("연동 해제는 204를 반환하고 서비스를 호출한다")
    void unlink() throws Exception {
        mockMvc.perform(delete("/eclass/link").header(FID_HEADER, "fid-1"))
                .andExpect(status().isNoContent());

        verify(eclassLinkService).unlink(eq("fid-1"), any());
    }

    @Test
    @DisplayName("수동 동기화는 204를 반환한다")
    void sync() throws Exception {
        mockMvc.perform(post("/eclass/sync").header(FID_HEADER, "fid-1"))
                .andExpect(status().isNoContent());

        verify(eclassLinkService).syncNow(eq("fid-1"), any());
    }

    @Test
    @DisplayName("연동이 끊긴 기기는 과제 없음이 아니라 만료 상태를 받는다")
    void getAssignmentsWhenExpired() throws Exception {
        when(eclassAssignmentService.getUpcoming(any(), any()))
                .thenReturn(EclassAssignmentListResponse.expired());

        mockMvc.perform(get("/eclass/assignments").header(FID_HEADER, "fid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true))
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andExpect(jsonPath("$.assignments").isEmpty());
    }
}
