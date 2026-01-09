package com.dongsoop.dongsoop.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.exception.FeedbackNotFoundException;
import com.dongsoop.dongsoop.feedback.repository.FeedbackRepository;
import com.dongsoop.dongsoop.feedback.service.FeedbackServiceImpl;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FeedbackDetailTest {

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Test
    void feedback_WhenDoesNotExist_ReturnsNotFound() throws Exception {
        // given
        when(feedbackRepository.searchFeedbackById(any(Long.class)))
                .thenReturn(Optional.empty());

        // when & then
        Assertions.assertThrows(FeedbackNotFoundException.class, () -> feedbackService.getFeedbackDetail(1L));
    }

    @Test
    void feedback_WhenExist_ReturnsFeedbackDetail() throws Exception {
        // given
        Long feedbackId = 1L;

        FeedbackDetail feedbackDetail = new FeedbackDetail(feedbackId,
                "This is a feedback content.",
                "This is a feature content.",
                1L,
                "name",
                LocalDateTime.now());

        when(feedbackRepository.searchFeedbackById(feedbackId))
                .thenReturn(Optional.of(feedbackDetail));

        // when & then
        assertThat(feedbackService.getFeedbackDetail(feedbackId))
                .isEqualTo(feedbackDetail);
    }
}
