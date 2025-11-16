package com.dongsoop.dongsoop.feedback.repository;

import com.dongsoop.dongsoop.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long>, FeedbackRepositoryCustom {
}
