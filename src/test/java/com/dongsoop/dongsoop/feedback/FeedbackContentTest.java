package com.dongsoop.dongsoop.feedback;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.feedback.controller.FeedbackController;
import com.dongsoop.dongsoop.feedback.service.FeedbackService;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.MethodArgumentNotValidException;

@WebMvcTest(controllers = FeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeedbackContentTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @Test
    @DisplayName("피드백이 1글자 미만일 때 예외와 400 에러를 던져야 한다.")
    void feedback_WhenContentTooShort_ReturnsBadRequest() throws Exception {
        // given

        // 1 글자 미만
        String improvementSuggestions = "";
        String featureRequests = "";

        // when
        ResultActions resultActions = request(improvementSuggestions, featureRequests);

        // then
        resultActions.andExpect(status().isBadRequest())
                .andExpect(result -> {
                    Throwable resolved = result.getResolvedException();
                    assertNotNull(resolved);
                    Assertions.assertInstanceOf(MethodArgumentNotValidException.class, resolved);
                });
    }

    @Test
    @DisplayName("피드백이 150글자 초과일 때 예외와 400 에러를 던져야 한다.")
    void feedback_WhenContentTooLong_ReturnsBadRequest() throws Exception {
        // given

        // 151 글자 (150글자 초과)
        String improvementSuggestions = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String featureRequests = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        // when
        ResultActions resultActions = request(improvementSuggestions, featureRequests);

        // then
        resultActions.andExpect(status().isBadRequest())
                .andExpect(result -> {
                    Throwable resolved = result.getResolvedException();
                    assertNotNull(resolved);
                    Assertions.assertInstanceOf(MethodArgumentNotValidException.class, resolved);
                });
    }

    @Test
    @DisplayName("피드백이 1글자 이상 150글자 이하일 때 201과 그에 맞는 경로가 반환되어야 한다.")
    void feedback_WhenContentValid_ReturnsCreated() throws Exception {
        // given
        String improvementSuggestions = "텍스트 크기가 좀 더 컸으면 좋겠습니다.";
        String featureRequests = "앱 잘 쓰고있습니다.";

        // when
        ResultActions resultActions = request(improvementSuggestions, featureRequests);

        // then
        resultActions.andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", matchesPattern(".*/feedback/\\d+$")));
    }

    ResultActions request(String improvementSuggestions, String featureRequests) throws Exception {
        String content = String.format("{\"improvementSuggestions\":\"%s\",\"featureRequests\":\"%s\"}",
                improvementSuggestions, featureRequests);

        return mockMvc.perform(post("/feedback")
                .contentType("application/json")
                .content(content));
    }
}
