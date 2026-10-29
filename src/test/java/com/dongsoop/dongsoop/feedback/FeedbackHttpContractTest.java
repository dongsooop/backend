package com.dongsoop.dongsoop.feedback;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dongsoop.dongsoop.feedback.controller.FeedbackController;
import com.dongsoop.dongsoop.feedback.dto.*;
import com.dongsoop.dongsoop.feedback.entity.ServiceFeature;
import com.dongsoop.dongsoop.feedback.service.FeedbackService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FeedbackHttpContractTest {
  @Mock FeedbackService service;
  @InjectMocks FeedbackController controller;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  @Test
  void creationReturnsSavedFeedbackLocationAndBindsSelection() throws Exception {
    var request = new FeedbackCreate(List.of(ServiceFeature.NOTICE_ALERT), "Improve", "Feature");
    when(service.submitFeedback(request)).thenReturn(19L);
    mvc.perform(
            post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"feature":["NOTICE_ALERT"],"improvementSuggestions":"Improve","featureRequests":"Feature"}
                """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/feedback/19"));
    verify(service).submitFeedback(request);
  }

  @Test
  void detailAndOverviewSerializeServiceResults() throws Exception {
    when(service.getFeedbackDetail(19L))
        .thenReturn(
            new FeedbackDetail(
                19L,
                "Improve",
                "Feature",
                7L,
                "Member",
                List.of(ServiceFeature.NOTICE_ALERT),
                LocalDateTime.of(2026, 10, 16, 12, 0)));
    when(service.getFeedbackOverview())
        .thenReturn(new FeedbackOverview(List.of(), List.of("Improve"), List.of("Feature")));
    mvc.perform(get("/feedback/19"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(19))
        .andExpect(jsonPath("$.serviceFeatureList[0]").value("NOTICE_ALERT"));
    mvc.perform(get("/feedback"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.improvementSuggestions[0]").value("Improve"));
  }

  @Test
  void suggestionListsBindPaging() throws Exception {
    var page = PageRequest.of(2, 5);
    when(service.getImprovementSuggestions(page)).thenReturn(List.of("Improve"));
    when(service.getFeatureRequests(page)).thenReturn(List.of("Feature"));
    mvc.perform(get("/feedback/improvement-suggestions").param("page", "2").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(content().json("[\"Improve\"]"));
    mvc.perform(get("/feedback/feature-requests").param("page", "2").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(content().json("[\"Feature\"]"));
  }

  @Test
  void invalidSelectionDoesNotSubmit() throws Exception {
    mvc.perform(
            post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"feature":[],"improvementSuggestions":"Improve","featureRequests":"Feature"}
                """))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
}
