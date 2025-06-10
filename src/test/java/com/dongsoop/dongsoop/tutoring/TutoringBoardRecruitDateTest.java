package com.dongsoop.dongsoop.tutoring;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.tutoring.controller.TutoringBoardController;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.recruitment.tutoring.service.TutoringBoardService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
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
class TutoringBoardRecruitDateTest {

    private static LocalDateTime standardDateTime;

    @MockitoBean
    private TutoringBoardRepository tutoringBoardRepository;

    @MockitoBean
    private DepartmentRepository departmentRepository;

    @MockitoBean
    private MemberService memberService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TutoringBoardService tutoringBoardService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @BeforeAll
    static void setUp() {
        standardDateTime = LocalDate.now()
                .atStartOfDay()
                .plusDays(1);
    }

    @BeforeEach
    void setUpEach() {
        TutoringBoard createdBoard = TutoringBoard.builder()
                .id(1L)
                .build();

        when(tutoringBoardService.create(any(CreateTutoringBoardRequest.class)))
                .thenReturn(createdBoard);
    }

    @Test
    @DisplayName("모집 시작 날짜가 종료 날짜 이후인 경우 예외를 던진다")
    void startRecruitment_WhenStartDateIsAfterEndDate_ThrowsMethodArgumentNotValidException() throws Exception {
        // given
        LocalDateTime endAt = standardDateTime; // 기준일 종료
        LocalDateTime startAt = standardDateTime.plusDays(1); // 기준일보다 하루 뒤 시작

        String jsonString = getJsonStringWithDate(startAt, endAt);

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        testInvalidDates(perform);
    }

    @Test
    @DisplayName("모집 시작일이 종료일과 같은 경우 예외를 던진다")
    void startRecruitment_WhenEndAtIsEqualsStartAt_ThrowsMethodArgumentNotValidException() throws Exception {
        // given
        String jsonString = getJsonStringWithDate(standardDateTime, standardDateTime); // 동일한 시작, 종료일

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        testInvalidDates(perform);
    }

    @Test
    @DisplayName("모집 시작 시간이 종료 시간 이후인 경우 예외를 던진다")
    void startRecruitment_WhenStartTimeIsAfterEndTime_ThrowsMethodArgumentNotValidException() throws Exception {
        // given
        LocalDateTime endAt = standardDateTime; // 기준일 00시 종료
        LocalDateTime startAt = endAt.plusHours(1); // 기준일 01시 시작

        String jsonString = getJsonStringWithDate(startAt, endAt);

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        testInvalidDates(perform);
    }

    @Test
    @DisplayName("모집 시작일이 종료일보다 과거인 경우 생성 상태를 반환한다")
    void startRecruitment_WhenEndAtIsAfterStartAt_ResponseCreated() throws Exception {
        // given
        LocalDateTime startAt = standardDateTime; // 기준일 시작
        LocalDateTime endAt = startAt.plusDays(1); // 기준일 하루 뒤 종료

        String jsonString = getJsonStringWithDate(startAt, endAt);

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        ResultMatcher created = status().isCreated();
        perform.andExpect(created);
    }

    @Test
    @DisplayName("모집 기간이 하루보다 짧은 경우 예외를 던진다")
    void startRecruitment_WhenDateIsLessThanADay_ThrowsMethodArgumentNotValidException() throws Exception {
        // given
        LocalDateTime startAt = standardDateTime.plusHours(14); // 기준일 14시 시작
        LocalDateTime endAt = startAt.plusHours(23); // 기준일 다음날 13시 종료

        String jsonString = getJsonStringWithDate(startAt, endAt);

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        testInvalidDates(perform);
    }

    @Test
    @DisplayName("모집 기간이 4주 보다 긴 경우 예외를 던진다")
    void startRecruitment_WhenDateIsMoreThanFourWeeks_ThrowsMethodArgumentNotValidException() throws Exception {
        // given
        LocalDateTime startAt = standardDateTime; // 오늘 00시 시작
        LocalDateTime endAt = startAt.plusWeeks(4).plusSeconds(1); // 4주 후 00시 1초 종료 (1초 초과)

        String jsonString = getJsonStringWithDate(startAt, endAt);

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        testInvalidDates(perform);
    }

    @Test
    @DisplayName("모집 기간이 하루인 경우 생성 상태를 응답한다")
    void startRecruitment_WhenDateIsADay_ResponseCreated() throws Exception {
        // given
        LocalDateTime startAt = LocalDate.now().atStartOfDay()
                .plusHours(14); // 오늘 14시 시작
        LocalDateTime endAt = startAt.plusDays(1); // 다음날 13시 종료

        String jsonString = getJsonStringWithDate(startAt, endAt);

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        ResultMatcher created = status().isCreated();
        perform.andExpect(created);
    }

    @Test
    @DisplayName("모집 기간이 4주인 경우 생성 상태를 응답한다")
    void startRecruitment_WhenDateIsFourWeeks_ResponseCreated() throws Exception {
        // given
        LocalDateTime startAt = LocalDate.now().atStartOfDay(); // 오늘 00시 시작
        LocalDateTime endAt = startAt.plusWeeks(4); // 4주 후 00시 종료

        String jsonString = getJsonStringWithDate(startAt, endAt);

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        ResultMatcher created = status().isCreated();
        perform.andExpect(created);
    }

    @Test
    @DisplayName("모집 시작일이 3개월이 지난 경우 예외를 던진다")
    void startRecruitment_WhenStartDateAfterThanThreeMonth_ThrowsMethodArgumentNotValidException() throws Exception {
        // given
        LocalDateTime startAt = LocalDateTime.now()
                .plusMonths(3)
                .plusDays(1); // 3개월 + 1일 후 시작
        LocalDateTime endAt = startAt.plusDays(1); // 시작일 다음날 종료

        String jsonString = getJsonStringWithDate(startAt, endAt);

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        testInvalidDates(perform);
    }

    @Test
    @DisplayName("모집 시작일이 정확히 3개월만 지난 경우 생성 상태를 응답한다")
    void startRecruitment_WhenStartDateAfterThreeMonth_ResponseCreated() throws Exception {
        // given
        LocalDateTime startAt = LocalDateTime.now()
                .plusMonths(3); // 3개월 후 시작
        LocalDateTime endAt = startAt.plusDays(1); // 시작일 다음날 종료

        String jsonString = getJsonStringWithDate(startAt, endAt);

        // when
        ResultActions perform = requestCreateTutoring(jsonString);

        // then
        ResultMatcher created = status().isCreated();
        perform.andExpect(created);
    }

    String getJsonStringWithDate(LocalDateTime startAt, LocalDateTime endAt) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("title", "title");
        json.put("tags", "tags");
        json.put("content", "content");
        JSONArray departmentTypeList = new JSONArray();
        departmentTypeList.put(DepartmentType.DEPT_2001.name());
        json.put("departmentTypeList", departmentTypeList);
        json.put("startAt", startAt.toString());
        json.put("endAt", endAt.toString());

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
