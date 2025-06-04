package com.dongsoop.dongsoop.studyBoard;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.recruitment.study.controller.StudyBoardController;
import com.dongsoop.dongsoop.recruitment.study.dto.CreateStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.study.service.StudyBoardServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.MethodArgumentNotValidException;

@WebMvcTest(controllers = StudyBoardController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudyBoardStartAtTest {

    private final JSONObject json = new JSONObject();

    private final String REQUEST_URL = "/study-board";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyBoardServiceImpl studyBoardService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() throws JSONException {
        json.put("title", "title");
        json.put("tags", "tags");
        json.put("content", "content");
        json.put("departmentTypeList",
                new JSONArray(new String[]{DepartmentType.DEPT_2001.name(), DepartmentType.DEPT_3001.name()}));
    }

    @Test
    @DisplayName("모집 게시판 생성 시 모집 시작일이 과거일 경우 예외를 던진다")
    void startRecruitment_WithPast_ShouldThrowException() throws Exception {
        // given

        LocalDateTime startAt = LocalDateTime.of(1999, 10, 1, 0, 0, 0);
        LocalDateTime endAt = LocalDateTime.of(1999, 10, 30, 23, 59, 59);

        json.put("startAt", startAt.toString());
        json.put("endAt", endAt.toString());

        // when
        MockHttpServletRequestBuilder content = post(REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.toString());

        // then
        MvcResult result = mockMvc.perform(content)
                .andExpect(status().isBadRequest())
                .andReturn();

        Throwable throwable = result.getResolvedException();
        assertNotNull(throwable);
        assertInstanceOf(MethodArgumentNotValidException.class, throwable);
    }

    @Test
    @DisplayName("모집 게시판 생성 시 모집 시작일이 오늘일 경우 성공 상태를 반환한다")
    void startRecruitment_WithToday_ShouldResponseSuccess() throws Exception {
        // given
        StudyBoard studyBoard = StudyBoard.builder()
                .id(1L)
                .title("title")
                .content("content")
                .tags("tags")
                .startAt(LocalDate.now().atStartOfDay())
                .endAt(LocalDate.now().atStartOfDay())
                .build();

        when(studyBoardService.create(any(CreateStudyBoardRequest.class)))
                .thenReturn(studyBoard);

        LocalDateTime startAt = LocalDate.now().atStartOfDay();
        LocalDateTime endAt = LocalDate.now().atStartOfDay().plusDays(1);

        json.put("startAt", startAt.toString());
        json.put("endAt", endAt.toString());

        // when
        MockHttpServletRequestBuilder httpContent = post(REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.toString());

        // then
        mockMvc.perform(httpContent)
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("모집 게시판 생성 시 모집 시작일이 null인 경우 예외를 던진다")
    void startRecruitment_WithNull_ShouldThrowException() throws Exception {
        // given
        json.put("startAt", JSONObject.NULL);
        json.put("endAt", JSONObject.NULL);

        // when
        MockHttpServletRequestBuilder content = post(REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.toString());

        // then
        MvcResult result = mockMvc.perform(content)
                .andExpect(status().isBadRequest())
                .andReturn();

        Throwable throwable = result.getResolvedException();
        assertNotNull(throwable);
        assertInstanceOf(MethodArgumentNotValidException.class, throwable);
    }
}
