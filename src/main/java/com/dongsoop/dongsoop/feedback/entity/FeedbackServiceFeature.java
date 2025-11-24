package com.dongsoop.dongsoop.feedback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackServiceFeature {

    @EmbeddedId
    private FeedbackServiceFeatureId id;

    public FeedbackServiceFeature(Feedback feedback, ServiceFeature serviceFeature) {
        this.id = new FeedbackServiceFeatureId(feedback, serviceFeature);
    }

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackServiceFeatureId {

        @JoinColumn(name = "feedback_id")
        @ManyToOne(fetch = FetchType.LAZY)
        private Feedback feedback;

        @Column(name = "service_feature")
        @Enumerated(EnumType.STRING)
        private ServiceFeature serviceFeature;
    }
}
