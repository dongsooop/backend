package com.dongsoop.dongsoop.notice.keyword;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.notice.keyword.controller.NoticeKeywordController;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordRequest;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordResponse;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeywordType;
import com.dongsoop.dongsoop.notice.keyword.exception.DuplicateNoticeKeywordException;
import com.dongsoop.dongsoop.notice.keyword.exception.NoticeKeywordNotFoundException;
import com.dongsoop.dongsoop.notice.keyword.service.NoticeKeywordService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NoticeKeywordController.class)
@AutoConfigureMockMvc(addFilters = false)
class NoticeKeywordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoticeKeywordService noticeKeywordService;

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

    @Test
    @DisplayName("키워드 목록 조회 시 200과 목록을 반환한다")
    void getKeywords_ReturnsOkWithList() throws Exception {
        given(noticeKeywordService.getKeywords()).willReturn(List.of(
                new NoticeKeywordResponse(1L, "장학", NoticeKeywordType.INCLUDE),
                new NoticeKeywordResponse(2L, "휴강", NoticeKeywordType.EXCLUDE)
        ));

        mockMvc.perform(get("/notice/keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].keyword").value("장학"))
                .andExpect(jsonPath("$[0].type").value("INCLUDE"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].keyword").value("휴강"))
                .andExpect(jsonPath("$[1].type").value("EXCLUDE"));
    }

    @Test
    @DisplayName("키워드가 없으면 빈 목록을 반환한다")
    void getKeywords_ReturnsEmptyList() throws Exception {
        given(noticeKeywordService.getKeywords()).willReturn(List.of());

        mockMvc.perform(get("/notice/keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("유효한 키워드 추가 요청 시 201을 반환한다")
    void addKeyword_WithValidRequest_ReturnsCreated() throws Exception {
        NoticeKeywordRequest request = new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE);
        NoticeKeywordResponse response = new NoticeKeywordResponse(1L, "장학", NoticeKeywordType.INCLUDE);

        given(noticeKeywordService.addKeyword(request)).willReturn(response);

        mockMvc.perform(post("/notice/keywords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.keyword").value("장학"))
                .andExpect(jsonPath("$.type").value("INCLUDE"));
    }

    @Test
    @DisplayName("keyword가 blank이면 400을 반환한다")
    void addKeyword_WithBlankKeyword_ReturnsBadRequest() throws Exception {
        NoticeKeywordRequest request = new NoticeKeywordRequest("  ", NoticeKeywordType.INCLUDE);

        mockMvc.perform(post("/notice/keywords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("type이 null이면 400을 반환한다")
    void addKeyword_WithNullType_ReturnsBadRequest() throws Exception {
        NoticeKeywordRequest request = new NoticeKeywordRequest("장학", null);

        mockMvc.perform(post("/notice/keywords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("키워드 20자 초과 시 400을 반환한다")
    void addKeyword_WithTooLongKeyword_ReturnsBadRequest() throws Exception {
        NoticeKeywordRequest request = new NoticeKeywordRequest("가".repeat(21), NoticeKeywordType.INCLUDE);

        mockMvc.perform(post("/notice/keywords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 키워드 추가 시 409를 반환한다")
    void addKeyword_WithDuplicateKeyword_ReturnsConflict() throws Exception {
        NoticeKeywordRequest request = new NoticeKeywordRequest("장학", NoticeKeywordType.INCLUDE);

        given(noticeKeywordService.addKeyword(request))
                .willThrow(new DuplicateNoticeKeywordException("장학"));

        mockMvc.perform(post("/notice/keywords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("키워드 삭제 성공 시 204를 반환한다")
    void deleteKeyword_WhenExists_ReturnsNoContent() throws Exception {
        willDoNothing().given(noticeKeywordService).deleteKeyword(1L);

        mockMvc.perform(delete("/notice/keywords/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 키워드 삭제 시 404를 반환한다")
    void deleteKeyword_WhenNotFound_ReturnsNotFound() throws Exception {
        willThrow(new NoticeKeywordNotFoundException(999L))
                .given(noticeKeywordService).deleteKeyword(999L);

        mockMvc.perform(delete("/notice/keywords/999"))
                .andExpect(status().isNotFound());
    }
}
