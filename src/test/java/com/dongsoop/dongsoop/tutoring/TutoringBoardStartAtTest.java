package com.dongsoop.dongsoop.tutoring;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.tutoring.controller.TutoringBoardController;
import com.dongsoop.dongsoop.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.tutoring.service.TutoringBoardServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

@WebMvcTest(controllers = TutoringBoardController.class)
@AutoConfigureMockMvc(addFilters = false)
class TutoringBoardStartAtTest {

    private final JSONObject json = new JSONObject();

    private final String REQUEST_URL = "/tutoring-board";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TutoringBoardServiceImpl tutoringBoardService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() throws JSONException {
        json.put("title", "title");
        json.put("tags", "tags");
        json.put("content", "content");
        json.put("departmentType", DepartmentType.DEPT_2001);
    }

    @Test
    @DisplayName("모집 게시판 생성 시 모집 시작일이 과거일 경우 예외를 던진다")
    void startRecruitment_WithPast_ShouldThrowException() throws Exception {
        // given
        json.put("startAt", LocalDateTime.of(1999, 10, 1, 0, 0, 0));
        json.put("endAt", LocalDateTime.of(1999, 10, 30, 23, 59, 59));

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
        DepartmentType departmentType = DepartmentType.DEPT_2001;
        Department department = new Department(departmentType, null, null);

        TutoringBoard tutoringBoard = TutoringBoard.builder()
                .id(1L)
                .title("title")
                .content("content")
                .tags("tags")
                .startAt(LocalDate.now().atStartOfDay())
                .endAt(LocalDate.now().atStartOfDay())
                .department(department)
                .build();

        when(tutoringBoardService.create(any(CreateTutoringBoardRequest.class)))
                .thenReturn(tutoringBoard);

        json.put("startAt", LocalDate.now().atStartOfDay());
        json.put("endAt", LocalDate.now().atStartOfDay().plusDays(1));

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
