package com.dongsoop.dongsoop.feedback.dto;

import com.dongsoop.dongsoop.feedback.entity.ServiceFeature;
import java.time.LocalDateTime;
import java.util.List;

public record FeedbackDetail(

        Long id,
        String improvementSuggestions,
        String featureRequests,
        Long memberId,
        String memberNickname,
        List<ServiceFeature> serviceFeatureList,
        LocalDateTime createdAt
) {
    public FeedbackDetail(Long id, String improvementSuggestions, String featureRequests, Long memberId,
                          String memberNickname, LocalDateTime createdAt) {
        this(
                id,
                improvementSuggestions,
                featureRequests,
                memberId,
                memberNickname,
                null,
                createdAt
        );
    }

    public FeedbackDetail fromBase(List<ServiceFeature> serviceFeatureList) {
        return new FeedbackDetail(
                this.id,
                this.improvementSuggestions,
                this.featureRequests,
                this.memberId,
                this.memberNickname,
                serviceFeatureList,
                this.createdAt
        );
    }
}
