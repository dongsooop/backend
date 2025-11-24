package com.dongsoop.dongsoop.feedback.repository;

import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.dto.FeedbackOverview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface FeedbackRepositoryCustom {

    Optional<FeedbackDetail> searchFeedbackById(Long id);

    FeedbackOverview searchFeedbackOverview();

    List<String> searchAllImprovementSuggestions(Pageable pageable);

    List<String> searchAllFeatureRequests(Pageable pageable);
}
