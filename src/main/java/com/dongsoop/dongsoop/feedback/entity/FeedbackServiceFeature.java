package com.dongsoop.dongsoop.feedback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@IdClass(FeedbackServiceFeature.FeedbackServiceFeatureId.class)
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackServiceFeature {

    @Id
    @JoinColumn(name = "feedback_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Feedback feedback;

    @Id
    @Column(name = "service_feature")
    @Enumerated(EnumType.STRING)
    private ServiceFeature serviceFeature;

    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackServiceFeatureId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long feedback;

        @Enumerated(EnumType.STRING)
        private ServiceFeature serviceFeature;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FeedbackServiceFeatureId that = (FeedbackServiceFeatureId) o;
            return Objects.equals(feedback, that.feedback) &&
                    serviceFeature == that.serviceFeature;
        }

        @Override
        public int hashCode() {
            return Objects.hash(feedback, serviceFeature);
        }
    }
}
