package com.dongsoop.dongsoop.feedback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackServiceFeature {

    @EmbeddedId
    private FeedbackServiceFeatureId id;

    public FeedbackServiceFeature(Long feedbackId, ServiceFeature serviceFeature) {
        this.id = new FeedbackServiceFeatureId(feedbackId, serviceFeature);
    }

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackServiceFeatureId {

        @Column(name = "feedback_id")
        private Long feedbackId;

        @Column(name = "service_feature")
        @Enumerated(EnumType.STRING)
        private ServiceFeature serviceFeature;
    }
}
