package com.dongsoop.dongsoop.feedback.repository;

import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.dto.FeedbackOverview;
import java.util.Optional;

public interface FeedbackRepositoryCustom {

    Optional<FeedbackDetail> searchFeedbackById(Long id);

    FeedbackOverview searchFeedbackOverview();
}
