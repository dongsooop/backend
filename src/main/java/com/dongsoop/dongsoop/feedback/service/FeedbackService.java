package com.dongsoop.dongsoop.feedback.service;

import com.dongsoop.dongsoop.feedback.dto.FeedbackCreate;

public interface FeedbackService {

    Long submitFeedback(FeedbackCreate request);
}
