package com.dongsoop.dongsoop.tutoring;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.recruitment.board.tutoring.controller.TutoringBoardController;
import com.dongsoop.dongsoop.recruitment.board.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.board.tutoring.service.TutoringBoardService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.MethodArgumentNotValidException;

@WebMvcTest(controllers = TutoringBoardController.class)
@AutoConfigureMockMvc(addFilters = false)
class TutoringBoardDepartmentSizeTest {

    @MockitoBean
    private TutoringBoardService tutoringBoardService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUpEach() {
        TutoringBoard createdBoard = TutoringBoard.builder()
                .id(1L)
                .build();

        when(tutoringBoardService.create(any(CreateTutoringBoardRequest.class)))
                .thenReturn(createdBoard);
    }

    @Test
    @DisplayName("학과가 1개일 때 생성 상태를 응답한다")
    void startRecruitment_WhenHasADepartment_ResponseCreated() throws Exception {
        // given
        List<Department> departmentList = List.of(new Department(DepartmentType.DEPT_2001, null, null));
        String jsonString = getJsonStringWithDepartment(departmentList);

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        ResultMatcher created = status().isCreated();
        perform.andExpect(created);
    }

    @Test
    @DisplayName("학과가 2개 이상일 때 예외를 던진다")
    void startRecruitment_WhenDepartmentIsMoreThanTwo_ThrowsMethodArgumentNotValidException() throws Exception {
        // given
        List<Department> departmentList = new ArrayList<>();
        Department departmentA = new Department(DepartmentType.DEPT_2001, null, null);
        Department departmentB = new Department(DepartmentType.DEPT_3001, null, null);
        departmentList.add(departmentA);
        departmentList.add(departmentB);
        String jsonString = getJsonStringWithDepartment(departmentList);

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        testInvalidDates(perform);
    }

    String getJsonStringWithDepartment(List<Department> departmentList) throws JSONException {
        LocalDateTime startAt = LocalDateTime.now().plusDays(1);
        LocalDateTime endAt = startAt.plusDays(2);

        JSONObject json = new JSONObject();
        json.put("title", "title");
        json.put("tags", "tags");
        json.put("content", "content");
        json.put("startAt", startAt.toString());
        json.put("endAt", endAt.toString());
        JSONArray departmentTypeList = new JSONArray();
        for (Department department : departmentList) {
            DepartmentType departmentType = department.getId();
            departmentTypeList.put(departmentType.name());
        }
        json.put("departmentTypeList", departmentTypeList);

        return json.toString();
    }

    void testInvalidDates(ResultActions perform) throws Exception {
        // then
        ResultMatcher badRequest = status().isBadRequest();
        MvcResult result = perform.andExpect(badRequest)
                .andReturn();

        assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException());
    }

    ResultActions requestCreateTutoring(String jsonTypeString) throws Exception {
        MockHttpServletRequestBuilder request = post("/tutoring-board")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonTypeString);

        // when
        return mockMvc.perform(request);
    }
}
