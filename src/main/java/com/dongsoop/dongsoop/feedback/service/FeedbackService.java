package com.dongsoop.dongsoop.feedback.service;

import com.dongsoop.dongsoop.feedback.dto.FeedbackCreate;
import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.dto.FeedbackOverview;

public interface FeedbackService {

    Long submitFeedback(FeedbackCreate request);

    FeedbackDetail getFeedbackDetail(Long id);

    FeedbackOverview getFeedbackOverview();
}
