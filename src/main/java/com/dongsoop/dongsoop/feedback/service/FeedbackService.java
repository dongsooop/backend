package com.dongsoop.dongsoop.feedback.service;

import com.dongsoop.dongsoop.feedback.dto.FeedbackCreate;
import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.dto.FeedbackOverview;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface FeedbackService {

    Long submitFeedback(FeedbackCreate request);

    FeedbackDetail getFeedbackDetail(Long id);

    FeedbackOverview getFeedbackOverview();

    List<String> getImprovementSuggestions(Pageable pageable);

    List<String> getFeatureRequests(Pageable pageable);
}
