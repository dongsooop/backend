package com.dongsoop.dongsoop.feedback.service;

import com.dongsoop.dongsoop.feedback.dto.FeedbackCreate;
import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;

public interface FeedbackService {

    Long submitFeedback(FeedbackCreate request);

    FeedbackDetail getFeedbackDetail(Long id);
}
