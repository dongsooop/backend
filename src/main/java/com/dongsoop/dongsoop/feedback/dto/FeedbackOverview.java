package com.dongsoop.dongsoop.feedback.dto;

import java.util.List;

public record FeedbackOverview(

        List<ServiceFeatureFeedback> serviceFeatures,
        List<String> improvementSuggestions,
        List<String> featureRequests
) {
}
