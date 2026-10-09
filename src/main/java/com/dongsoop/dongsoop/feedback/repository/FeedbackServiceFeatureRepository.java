package com.dongsoop.dongsoop.feedback.repository;

import com.dongsoop.dongsoop.feedback.entity.FeedbackServiceFeature;
import com.dongsoop.dongsoop.feedback.entity.FeedbackServiceFeature.FeedbackServiceFeatureId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackServiceFeatureRepository extends JpaRepository<FeedbackServiceFeature, FeedbackServiceFeatureId> {
}
